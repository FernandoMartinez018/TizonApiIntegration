package com.lfmm.tizon_integration.config;

import com.lfmm.tizon_integration.TizonIntegrationApplication;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * RNF-SEG-02: la API no arranca si falta un secreto obligatorio, y el error lo dice claramente.
 */
class RequiredSecretsStartupTest {

    @ParameterizedTest(name = "{0} vacío → {1}")
    @CsvSource({
            "jwt.secret,                  JWT_SECRET es obligatorio",
            "jwt.secret=corto,            JWT_SECRET debe tener al menos 32 bytes",
            "ia.internal-token,           INTERNAL_API_TOKEN es obligatorio",
            "ia.base-url,                 IA_BASE_URL es obligatorio",
            "spring.datasource.password,  DB_PASS es obligatorio",
    })
    void noArrancaSinSecreto(String property, String expectedMessage) {
        String override = property.contains("=") ? property : property + "=";

        // Argumentos de línea de comandos: tienen prioridad sobre application-test.yml.
        assertThatThrownBy(() -> new SpringApplicationBuilder(TizonIntegrationApplication.class)
                .profiles("test")
                .web(WebApplicationType.NONE)
                .run("--" + override, "--spring.main.banner-mode=off")
                .close())
                .hasStackTraceContaining(expectedMessage);
    }
}
