package com.lfmm.tizon_integration.config;

import com.lfmm.tizon_integration.TizonIntegrationApplication;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
            "tizon.admin.username,        define ADMIN_USERNAME y ADMIN_PASSWORD",
            "tizon.admin.password=corta,  ADMIN_PASSWORD debe tener al menos 8 caracteres",
    })
    void noArrancaSinSecreto(String property, String expectedMessage) {
        String override = property.contains("=") ? property : property + "=";
        // BD propia: la H2 compartida de las otras pruebas ya tiene un ADMIN creado.
        String freshDb = "--spring.datasource.url=jdbc:h2:mem:" + java.util.UUID.randomUUID()
                + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH";

        // Argumentos de línea de comandos: tienen prioridad sobre application-test.yml.
        assertThatThrownBy(() -> new SpringApplicationBuilder(TizonIntegrationApplication.class)
                .profiles("test")
                .run("--" + override, freshDb, "--server.port=0", "--spring.main.banner-mode=off")
                .close())
                .hasStackTraceContaining(expectedMessage);
    }
}
