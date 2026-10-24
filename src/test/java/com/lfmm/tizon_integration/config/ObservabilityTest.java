package com.lfmm.tizon_integration.config;

import com.lfmm.tizon_integration.model.AppUser.Role;
import com.lfmm.tizon_integration.support.TestUsers;
import io.micrometer.core.instrument.MeterRegistry;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
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

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** F3-API-04: health compuesto con la IA, X-Request-Id de punta a punta y métricas de la IA. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestUsers.class)
class ObservabilityTest {

    /** IA simulada cuyo estado cambia según la prueba. */
    private static final MockWebServer ia = new MockWebServer();
    private static volatile boolean iaHealthy = true;

    static {
        ia.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                if ("/health".equals(request.getPath()) && iaHealthy) {
                    return new MockResponse().addHeader("Content-Type", "application/json").setBody(
                            "{\"status\":\"ok\",\"model_loaded\":true,\"model_version\":\"v-test\",\"classes\":[],\"input_spec\":{}}");
                }
                return new MockResponse().setResponseCode(503).setBody("{}");
            }
        });
        try {
            ia.start();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @DynamicPropertySource
    static void iaUrl(DynamicPropertyRegistry registry) {
        registry.add("ia.base-url", () -> ia.url("/").toString());
    }

    @AfterAll
    static void stop() throws IOException {
        ia.shutdown();
    }

    @Autowired MockMvc mvc;
    @Autowired TestUsers testUsers;
    @Autowired MeterRegistry meterRegistry;

    @Test
    void healthEsDownSiLaIaEstaCaida() throws Exception {
        iaHealthy = true;
        mvc.perform(get("/actuator/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));

        iaHealthy = false;
        try {
            mvc.perform(get("/actuator/health")).andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.status").value("DOWN"))
                    .andExpect(jsonPath("$.components").doesNotExist());   // sin detalles en público
        } finally {
            iaHealthy = true;
        }
    }

    @Test
    void requestIdSeGeneraSeConservaOSeReemplaza() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(header().string("X-Request-Id", matchesPattern("[0-9a-f-]{36}")));
        mvc.perform(get("/actuator/health").header("X-Request-Id", "app-123"))
                .andExpect(header().string("X-Request-Id", "app-123"));
        mvc.perform(get("/actuator/health").header("X-Request-Id", "<script>"))
                .andExpect(header().string("X-Request-Id", matchesPattern("[0-9a-f-]{36}")));
    }

    @Test
    void elErrorIncluyeElRequestIdYSeCuentaEnLasMetricas() throws Exception {
        var user = testUsers.create(Role.AGRICULTOR);
        String bearer = testUsers.bearer(user);
        String body = mvc.perform(post("/sessions").header("Authorization", bearer))
                .andReturn().getResponse().getContentAsString();
        String sessionId = body.replaceAll(".*\"session_id\":\"([^\"]+)\".*", "$1");

        mvc.perform(post("/sessions/" + sessionId + "/frames").header("Authorization", bearer)
                        .header("X-Request-Id", "req-err-1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"frame_b64\":\"" + "A".repeat(200) + "\"}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.request_id").value("req-err-1"));

        assertThat(meterRegistry.counter("tizon.ia.errors", "code", "IA_UNAVAILABLE").count()).isGreaterThanOrEqualTo(1);
        assertThat(meterRegistry.timer("tizon.ia.latency").count()).isGreaterThanOrEqualTo(1);
    }
}
