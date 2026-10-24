package com.lfmm.tizon_integration.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lfmm.tizon_integration.dto.ErrorCode;
import com.lfmm.tizon_integration.dto.IaAnalyzeResponse;
import com.lfmm.tizon_integration.dto.Probabilities;
import com.lfmm.tizon_integration.dto.Region;
import com.lfmm.tizon_integration.exception.ApiException;
import com.lfmm.tizon_integration.model.AppUser;
import com.lfmm.tizon_integration.model.AppUser.Role;
import com.lfmm.tizon_integration.model.ClassId;
import com.lfmm.tizon_integration.model.FrameStatus;
import com.lfmm.tizon_integration.model.ScanSession;
import com.lfmm.tizon_integration.model.ScanSession.SessionStatus;
import com.lfmm.tizon_integration.repository.ScanResultRepository;
import com.lfmm.tizon_integration.repository.ScanSessionRepository;
import com.lfmm.tizon_integration.service.IaClient;
import com.lfmm.tizon_integration.service.SessionService;
import com.lfmm.tizon_integration.support.TestUsers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** F3-API-01 / RF-04: ciclo de vida de la sesión y propiedad (ajeno → 404, no ACTIVE → 409). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestUsers.class)
class SessionFlowTest {

    private static final String FRAME = "{\"frame_b64\":\"" + "A".repeat(200) + "\"}";
    private static final Probabilities TIZON = new Probabilities(0.1, null, 0.9);
    private static final IaAnalyzeResponse DETECTED = new IaAnalyzeResponse(FrameStatus.DETECTED, ClassId.TIZON_TARDIO,
            0.9, TIZON, List.of(new Region(0.1, 0.1, 0.5, 0.5, ClassId.TIZON_TARDIO, 0.9, TIZON)), 640, 480, "test-v1", 120);

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired TestUsers testUsers;
    @Autowired ScanSessionRepository sessionRepo;
    @Autowired ScanResultRepository resultRepo;
    @Autowired SessionService sessionService;
    @MockitoBean IaClient iaClient;

    private AppUser ana;
    private AppUser beto;
    private AppUser tecnico;

    @BeforeEach
    void setUp() {
        ana = testUsers.create(Role.AGRICULTOR);
        beto = testUsers.create(Role.AGRICULTOR);
        tecnico = testUsers.create(Role.TECNICO);
        when(iaClient.analyze(anyString(), any())).thenReturn(DETECTED);
    }

    private ResultActions as(AppUser user, org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder req) throws Exception {
        return mvc.perform(req.header("Authorization", testUsers.bearer(user)).contentType(MediaType.APPLICATION_JSON));
    }

    private UUID start(AppUser user) throws Exception {
        String body = as(user, post("/sessions")).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(json.readTree(body).get("session_id").asText());
    }

    private ResultActions frame(AppUser user, UUID sessionId) throws Exception {
        return as(user, post("/sessions/" + sessionId + "/frames").content(FRAME));
    }

    @Test
    void iniciarConUbicacionYFincaUsaElUsuarioDelJwt() throws Exception {
        as(ana, post("/sessions").content("""
                {"location":{"latitude":4.71,"longitude":-74.07,"accuracy_m":12},"field_name":" Lote 3 "}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.username").value(ana.getUsername()))
                .andExpect(jsonPath("$.field_name").value("Lote 3"))
                .andExpect(jsonPath("$.location.accuracy_m").value(12.0))
                .andExpect(jsonPath("$.severity").value("NONE"));
    }

    @Test
    void elDuenoEnviaFramesYRecibeEstadisticas() throws Exception {
        UUID id = start(ana);
        frame(ana, id).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DETECTED"))
                .andExpect(jsonPath("$.label").value("TIZON_TARDIO"))
                .andExpect(jsonPath("$.probabilities.TIZON_TARDIO").value(0.9))
                .andExpect(jsonPath("$.regions[0].label").value("TIZON_TARDIO"))
                .andExpect(jsonPath("$.session_stats.affected_frames").value(1))
                .andExpect(jsonPath("$.session_stats.severity").value("CRITICAL"));
        assertThat(resultRepo.countBySessionId(id)).isEqualTo(1);
    }

    @Test
    void noVegetationEsUnResultadoQueSePersiste() throws Exception {
        when(iaClient.analyze(anyString(), any())).thenReturn(new IaAnalyzeResponse(
                FrameStatus.NO_VEGETATION, null, null, null, List.of(), 640, 480, "test-v1", 50));
        UUID id = start(ana);
        frame(ana, id).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NO_VEGETATION"))
                .andExpect(jsonPath("$.session_stats.no_vegetation_frames").value(1))
                .andExpect(jsonPath("$.session_stats.analyzed_frames").value(0));
        assertThat(resultRepo.countBySessionId(id)).isEqualTo(1);
    }

    @Test
    void hojaDeOtraPlantaSePersisteSinAnalisisDeTizon() throws Exception {
        when(iaClient.analyze(anyString(), any())).thenReturn(new IaAnalyzeResponse(
                FrameStatus.NOT_POTATO, null, null, null, List.of(), 640, 480, "test-v1", 50));
        UUID id = start(ana);
        frame(ana, id).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NOT_POTATO"))
                .andExpect(jsonPath("$.label").doesNotExist())
                .andExpect(jsonPath("$.session_stats.not_potato_frames").value(1))
                .andExpect(jsonPath("$.session_stats.analyzed_frames").value(0));
        assertThat(resultRepo.countBySessionId(id)).isEqualTo(1);
    }

    @Test
    void tizonTempranoSeCuentaComoAfectadoYPorClase() throws Exception {
        Probabilities early = new Probabilities(0.05, 0.88, 0.04);
        when(iaClient.analyze(anyString(), any())).thenReturn(new IaAnalyzeResponse(FrameStatus.DETECTED,
                ClassId.TIZON_TEMPRANO, 0.88, early, List.of(new Region(0.1, 0.1, 0.5, 0.5, ClassId.TIZON_TEMPRANO, 0.88,
                early)), 640, 480, "test-v1", 80));
        UUID id = start(ana);
        frame(ana, id).andExpect(status().isOk())
                .andExpect(jsonPath("$.label").value("TIZON_TEMPRANO"))
                .andExpect(jsonPath("$.probabilities.TIZON_TEMPRANO").value(0.88))
                .andExpect(jsonPath("$.session_stats.affected_frames").value(1))
                .andExpect(jsonPath("$.session_stats.label_counts.TIZON_TEMPRANO").value(1))
                .andExpect(jsonPath("$.session_stats.label_counts.TIZON_TARDIO").value(0))
                .andExpect(jsonPath("$.session_stats.dominant_label").value("TIZON_TEMPRANO"));
        assertThat(resultRepo.findAll()).anySatisfy(r -> assertThat(r.getProbTizonTemprano()).isEqualTo(0.88));
    }

    @Test
    void sesionAjenaOInexistenteResponde404SinLlamarALaIa() throws Exception {
        UUID id = start(ana);
        frame(beto, id).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("NOT_FOUND"));
        frame(ana, UUID.randomUUID()).andExpect(status().isNotFound());
        as(beto, post("/sessions/" + id + "/end")).andExpect(status().isNotFound());
        as(beto, get("/sessions/" + id)).andExpect(status().isNotFound());
        verify(iaClient, never()).analyze(anyString(), any());
    }

    @Test
    void sesionFinalizadaResponde409YNoCreaUnaNueva() throws Exception {
        UUID id = start(ana);
        frame(ana, id).andExpect(status().isOk());
        as(ana, post("/sessions/" + id + "/end")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.ended_at").isNotEmpty())
                .andExpect(jsonPath("$.recommendations[0].text").value("Consulta a un técnico agrónomo"))
                .andExpect(jsonPath("$.recommendations[0].source").isNotEmpty());
        long sessionsBefore = sessionRepo.count();

        frame(ana, id).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("SESSION_NOT_ACTIVE"));
        as(ana, post("/sessions/" + id + "/end")).andExpect(status().isConflict());
        assertThat(sessionRepo.count()).isEqualTo(sessionsBefore);
        assertThat(resultRepo.countBySessionId(id)).isEqualTo(1);
    }

    @Test
    void descartarDejaLaSesionCancelada() throws Exception {
        UUID id = start(ana);
        as(ana, post("/sessions/" + id + "/cancel")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        frame(ana, id).andExpect(status().isConflict());
    }

    @Test
    void errorDeLaIaNoPersisteNada() throws Exception {
        when(iaClient.analyze(anyString(), any())).thenThrow(new ApiException(
                HttpStatus.BAD_GATEWAY, ErrorCode.IA_UNAVAILABLE, "no disponible"));
        UUID id = start(ana);
        frame(ana, id).andExpect(status().isBadGateway()).andExpect(jsonPath("$.code").value("IA_UNAVAILABLE"));
        assertThat(resultRepo.countBySessionId(id)).isZero();
        assertThat(sessionRepo.findById(id).orElseThrow().getTotalFrames()).isZero();
    }

    @Test
    void historialPropioYFiltroSoloParaTecnico() throws Exception {
        UUID propia = start(ana);
        start(beto);

        JsonNode page = json.readTree(as(ana, get("/sessions?size=100")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertThat(page.get("items")).allSatisfy(s -> assertThat(s.get("username").asText()).isEqualTo(ana.getUsername()));
        assertThat(page.get("items").get(0).get("session_id").asText()).isEqualTo(propia.toString());

        as(ana, get("/sessions?username=" + beto.getUsername())).andExpect(status().isForbidden());

        as(tecnico, get("/sessions?username=" + beto.getUsername())).andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].username").value(beto.getUsername()));
        as(tecnico, get("/sessions/" + propia)).andExpect(status().isOk());
        as(ana, get("/sessions?size=101")).andExpect(status().isBadRequest());
    }

    @Test
    void sesionesAbandonadasPasanACancelled() throws Exception {
        UUID vieja = start(ana);
        UUID reciente = start(ana);
        ScanSession s = sessionRepo.findById(vieja).orElseThrow();
        s.setLastFrameAt(Instant.now().minus(Duration.ofMinutes(31)));
        sessionRepo.save(s);

        sessionService.cancelInactive(Instant.now());

        assertThat(sessionRepo.findById(vieja).orElseThrow().getStatus()).isEqualTo(SessionStatus.CANCELLED);
        assertThat(sessionRepo.findById(reciente).orElseThrow().getStatus()).isEqualTo(SessionStatus.ACTIVE);
    }
}
