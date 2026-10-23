package com.lfmm.tizon_integration.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RNF-SEG-03 / RNF-SEG-05: toda ruta exige JWT salvo login, registro y health; 401/403 en JSON.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTest {

    private static final String PROTECTED_ROUTES = """
            POST, /analysis/frame
            POST, /analysis/sessions/start
            POST, /analysis/sessions/00000000-0000-0000-0000-000000000000/end
            GET,  /analysis/sessions/history
            GET,  /actuator/info
            GET,  /actuator/metrics
            GET,  /ruta/inexistente
            """;

    @Autowired MockMvc mvc;
    @Autowired JwtTokenProvider tokenProvider;
    @Autowired JsonAccessDeniedHandler accessDeniedHandler;

    @ParameterizedTest(name = "{0} {1} sin token → 401")
    @CsvSource(textBlock = PROTECTED_ROUTES)
    void sinTokenResponde401Json(String method, String path) throws Exception {
        mvc.perform(request(HttpMethod.valueOf(method), path)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @ParameterizedTest(name = "{0} {1} con token → no 401")
    @CsvSource(textBlock = PROTECTED_ROUTES)
    void conTokenNoResponde401(String method, String path) throws Exception {
        int status = mvc.perform(request(HttpMethod.valueOf(method), path)
                        .header("Authorization", "Bearer " + tokenProvider.generateToken("admin"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andReturn().getResponse().getStatus();

        assertThat(status).isNotEqualTo(401);
    }

    @Test
    void tokenInvalidoResponde401() throws Exception {
        mvc.perform(request(HttpMethod.GET, "/analysis/sessions/history")
                        .header("Authorization", "Bearer no.es.un.jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void rutasPublicasNoExigenToken() throws Exception {
        mvc.perform(request(HttpMethod.GET, "/actuator/health")).andExpect(status().isOk());
        // Cuerpo vacío: la validación responde 400, prueba de que pasó la capa de seguridad.
        mvc.perform(request(HttpMethod.POST, "/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void accesoDenegadoResponde403Json() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        accessDeniedHandler.handle(new MockHttpServletRequest(), response, new AccessDeniedException("x"));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(new ObjectMapper().readTree(response.getContentAsString()).get("code").asText())
                .isEqualTo("FORBIDDEN");
    }
}
