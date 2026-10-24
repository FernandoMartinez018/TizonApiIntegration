package com.lfmm.tizon_integration.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lfmm.tizon_integration.dto.IaAnalyzeResponse;
import com.lfmm.tizon_integration.dto.Probabilities;
import com.lfmm.tizon_integration.model.AppUser;
import com.lfmm.tizon_integration.model.AppUser.Role;
import com.lfmm.tizon_integration.model.ClassId;
import com.lfmm.tizon_integration.model.FrameStatus;
import com.lfmm.tizon_integration.repository.ScanSessionRepository;
import com.lfmm.tizon_integration.service.AlertService;
import com.lfmm.tizon_integration.service.IaClient;
import com.lfmm.tizon_integration.support.TestUsers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** F6-API-02 / RF-11: regla de alerta, una por sesión, visibilidad por rol y ack solo de TECNICO/ADMIN. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestUsers.class)
class AlertFlowTest {

    private static final String FRAME = "{\"frame_b64\":\"" + "A".repeat(200) + "\"}";
    private static final Probabilities P = new Probabilities(0.1, 0.9);

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired TestUsers testUsers;
    @Autowired ScanSessionRepository sessionRepo;
    @Autowired AlertService alertService;
    @MockitoBean IaClient iaClient;

    private AppUser ana;
    private AppUser beto;
    private AppUser tecnico;

    @BeforeEach
    void setUp() {
        ana = testUsers.create(Role.AGRICULTOR);
        beto = testUsers.create(Role.AGRICULTOR);
        tecnico = testUsers.create(Role.TECNICO);
        when(iaClient.analyze(anyString(), any())).thenReturn(new IaAnalyzeResponse(
                FrameStatus.DETECTED, ClassId.TIZON_TARDIO, 0.9, P, List.of(), 640, 480, "t", 10));
    }

    private org.springframework.test.web.servlet.ResultActions as(AppUser u, MockHttpServletRequestBuilder req) throws Exception {
        return mvc.perform(req.header("Authorization", testUsers.bearer(u)).contentType(MediaType.APPLICATION_JSON));
    }

    /** Sesión de [user] con [frames] frames TIZON_TARDIO (100 % afectado), ya finalizada. Devuelve el JSON final. */
    private com.fasterxml.jackson.databind.JsonNode scanAndEnd(AppUser user, int frames) throws Exception {
        String created = as(user, post("/sessions").content("{\"field_name\":\"Lote 7\"}")).andReturn().getResponse().getContentAsString();
        UUID id = UUID.fromString(json.readTree(created).get("session_id").asText());
        for (int i = 0; i < frames; i++) as(user, post("/sessions/" + id + "/frames").content(FRAME)).andExpect(status().isOk());
        return json.readTree(as(user, post("/sessions/" + id + "/end")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
    }

    @ParameterizedTest(name = "{0} frames, {1} % → alerta={2}")
    @CsvSource({"5, 30, true", "5, 29.99, false", "4, 100, false", "20, 100, true"})
    void reglaDeAlerta(int analyzed, double pct, boolean expected) {
        assertThat(AlertService.meetsRule(analyzed, pct, 30, 5)).isEqualTo(expected);
    }

    @Test
    void sesionQueSuperaElUmbralGeneraUnaAlertaVisibleParaElDuenoYElTecnico() throws Exception {
        var end = scanAndEnd(ana, 5);
        String alertId = end.get("alert_id").asText();
        assertThat(alertId).isNotBlank();

        as(ana, get("/alerts")).andExpect(status().isOk())
                .andExpect(jsonPath("$[*].alert_id", hasItem(alertId)))
                .andExpect(jsonPath("$[0].field_name").value("Lote 7"))
                .andExpect(jsonPath("$[0].status").value("OPEN"));
        as(beto, get("/alerts")).andExpect(status().isOk()).andExpect(jsonPath("$[*].alert_id", not(hasItem(alertId))));
        as(tecnico, get("/alerts?status=OPEN")).andExpect(status().isOk()).andExpect(jsonPath("$[*].alert_id", hasItem(alertId)));

        // El detalle de la sesión también informa la alerta.
        as(ana, get("/sessions/" + end.get("session_id").asText())).andExpect(jsonPath("$.alert_id").value(alertId));
    }

    @Test
    void menosDeCincoFramesNoGeneraAlerta() throws Exception {
        var end = scanAndEnd(ana, 4);
        assertThat(end.has("alert_id")).isFalse();
    }

    @Test
    void unaSolaAlertaPorSesion() throws Exception {
        var end = scanAndEnd(ana, 6);
        var session = sessionRepo.findById(UUID.fromString(end.get("session_id").asText())).orElseThrow();
        assertThat(alertService.createIfNeeded(session).orElseThrow().getId().toString())
                .isEqualTo(end.get("alert_id").asText());
    }

    @Test
    void soloTecnicoOAdminMarcanComoAtendida() throws Exception {
        String alertId = scanAndEnd(ana, 5).get("alert_id").asText();

        as(ana, post("/alerts/" + alertId + "/ack")).andExpect(status().isForbidden());
        as(tecnico, post("/alerts/" + alertId + "/ack")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACKNOWLEDGED"))
                .andExpect(jsonPath("$.acknowledged_by").value(tecnico.getUsername()));
        // Idempotente
        as(tecnico, post("/alerts/" + alertId + "/ack")).andExpect(status().isOk());
        as(tecnico, post("/alerts/" + UUID.randomUUID() + "/ack")).andExpect(status().isNotFound());
        as(ana, get("/alerts?status=OPEN")).andExpect(jsonPath("$[*].alert_id", not(hasItem(alertId))));
    }

    @Test
    void sinceFiltraLasAlertasNuevas() throws Exception {
        String alertId = scanAndEnd(ana, 5).get("alert_id").asText();
        as(ana, get("/alerts?since=" + Instant.now().plusSeconds(60))).andExpect(jsonPath("$[*].alert_id", not(hasItem(alertId))));
        as(ana, get("/alerts?since=" + Instant.now().minusSeconds(600))).andExpect(jsonPath("$[*].alert_id", hasItem(alertId)));
    }
}
