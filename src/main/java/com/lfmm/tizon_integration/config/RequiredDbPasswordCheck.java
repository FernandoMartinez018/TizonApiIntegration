package com.lfmm.tizon_integration.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.StringUtils;

/**
 * Exige DB_PASS antes de crear cualquier bean. Como @ConfigurationProperties no basta aquí:
 * el pool de conexiones se abriría antes y el error sería un confuso "password incorrecto".
 * Registrado en META-INF/spring.factories.
 */
public class RequiredDbPasswordCheck implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment env, SpringApplication app) {
        if (!StringUtils.hasText(env.getProperty("spring.datasource.password"))) {
            throw new IllegalStateException("DB_PASS es obligatorio (spring.datasource.password vacío)");
        }
    }
}
