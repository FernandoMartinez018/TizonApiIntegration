package com.lfmm.tizon_integration.service;

import com.lfmm.tizon_integration.config.IaProperties;
import com.lfmm.tizon_integration.config.WebClientConfig;
import com.lfmm.tizon_integration.dto.ErrorCode;
import com.lfmm.tizon_integration.exception.ApiException;
import com.lfmm.tizon_integration.model.AppUser;
import com.lfmm.tizon_integration.model.AppUser.Role;
import com.lfmm.tizon_integration.repository.ScanResultRepository;
import com.lfmm.tizon_integration.repository.ScanSessionRepository;
import com.lfmm.tizon_integration.support.TestUsers;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * F3-API-02 / RF-07: Spring → IA real por HTTP contra una IA simulada (MockWebServer).
 * 200 se persiste; 400, 503, timeout y conexión rechazada NO cambian scan_results.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestUsers.class)
class IaClientIntegrationTest {

    private static final MockWebServer ia = new MockWebServer();
    private static final String FRAME = "{\"frame_b64\":\"" + "A".repeat(200) + "\"}";
    private static final String IA_OK = """
            {"status":"DETECTED","label":"SANO","confidence":0.95,
             "probabilities":{"SANO":0.95,"TIZON_TARDIO":0.05},
             "regions":[{"x":0.1,"y":0.2,"width":0.3,"height":0.4,"label":"SANO","confidence":0.95,
                         "probabilities":{"SANO":0.95,"TIZON_TARDIO":0.05}}],
             "frame_width":640,"frame_height":480,"model_version":"mobilenetv2-test","inference_ms":210}""";

    static {
        try {
            ia.start();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @DynamicPropertySource
    static void iaUrl(DynamicPropertyRegistry registry) {
        registry.add("ia.base-url", () -> ia.url("/").toString());
        registry.add("ia.timeout-seconds", () -> 1);
    }

    @AfterAll
    static void stop() throws IOException {
        ia.shutdown();
    }

    @Autowired MockMvc mvc;
    @Autowired TestUsers testUsers;
    @Autowired ScanResultRepository resultRepo;
    @Autowired ScanSessionRepository sessionRepo;

    private AppUser user;
    private UUID sessionId;

    @BeforeEach
    void startSession() throws Exception {
        // Descarta peticiones pendientes de otra prueba.
        while (ia.takeRequest(10, TimeUnit.MILLISECONDS) != null) { /* vaciar */ }
        user = testUsers.create(Role.AGRICULTOR);
        String body = mvc.perform(post("/sessions").header("Authorization", testUsers.bearer(user)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        sessionId = UUID.fromString(body.replaceAll(".*\"session_id\":\"([^\"]+)\".*", "$1"));
    }

    private ResultActions sendFrame() throws Exception {
        return mvc.perform(post("/sessions/" + sessionId + "/frames")
                .header("Authorization", testUsers.bearer(user))
                .header("X-Request-Id", "req-test-1")
                .contentType(MediaType.APPLICATION_JSON).content(FRAME));
    }

    private void assertNothingPersisted() {
        assertThat(resultRepo.countBySessionId(sessionId)).isZero();
        assertThat(sessionRepo.findById(sessionId).orElseThrow().getTotalFrames()).isZero();
    }

    @Test
    void respuesta200SePersisteYPropagaElTokenInterno() throws Exception {
        ia.enqueue(new MockResponse().setBody(IA_OK).addHeader("Content-Type", "application/json"));

        sendFrame().andExpect(status().isOk())
                .andExpect(jsonPath("$.label").value("SANO"))
                .andExpect(jsonPath("$.model_version").value("mobilenetv2-test"));

        RecordedRequest request = ia.takeRequest(1, TimeUnit.SECONDS);
        assertThat(request.getPath()).isEqualTo("/v1/analyze");
        assertThat(request.getHeader("X-Internal-Token")).isEqualTo("test-internal-token");
        assertThat(request.getHeader("X-Request-Id")).as("se propaga el X-Request-Id de la app").isEqualTo("req-test-1");
        assertThat(request.getBody().readUtf8()).contains("\"frame_b64\"");
        assertThat(resultRepo.countBySessionId(sessionId)).isEqualTo(1);
    }

    @Test
    void ia400EsInvalidImage() throws Exception {
        ia.enqueue(new MockResponse().setResponseCode(400).setBody("{\"code\":\"INVALID_IMAGE\"}"));
        sendFrame().andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_IMAGE"));
        assertNothingPersisted();
    }

    @Test
    void ia503EsIaUnavailableSinReintentar() throws Exception {
        ia.enqueue(new MockResponse().setResponseCode(503).setBody("{\"code\":\"MODEL_NOT_READY\"}"));
        sendFrame().andExpect(status().isBadGateway()).andExpect(jsonPath("$.code").value("IA_UNAVAILABLE"));
        assertNothingPersisted();
        assertThat(ia.takeRequest(1, TimeUnit.SECONDS)).isNotNull();
        assertThat(ia.takeRequest(200, TimeUnit.MILLISECONDS)).as("no reintenta ante un error HTTP").isNull();
    }

    @Test
    void timeoutEsIaTimeout() throws Exception {
        ia.enqueue(new MockResponse().setBody(IA_OK).addHeader("Content-Type", "application/json")
                .setHeadersDelay(3, TimeUnit.SECONDS));
        sendFrame().andExpect(status().isGatewayTimeout()).andExpect(jsonPath("$.code").value("IA_TIMEOUT"));
        assertNothingPersisted();
    }

    @Test
    void conexionRechazadaEsIaUnavailable() throws Exception {
        // Puerto cerrado: un servidor que se apaga de inmediato.
        MockWebServer closed = new MockWebServer();
        closed.start();
        String url = closed.url("/").toString();
        closed.shutdown();

        IaProperties props = new IaProperties(url, 1, "x");
        IaClient client = new IaClient(new WebClientConfig().iaWebClient(WebClient.builder(), props), props,
                new io.micrometer.core.instrument.simple.SimpleMeterRegistry());

        assertThatThrownBy(() -> client.analyze("A".repeat(200), "req"))
                .isInstanceOfSatisfying(ApiException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ErrorCode.IA_UNAVAILABLE));
    }
}
