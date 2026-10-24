package com.lfmm.tizon_integration.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * F2-API-01: los endpoints expuestos por Spring (springdoc) coinciden con los del contrato
 * (contracts/tizon-api.yaml, copia de la fuente de verdad de la carpeta raíz).
 */
@SpringBootTest(properties = "springdoc.api-docs.enabled=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiContractTest {

    /** Rutas del contrato servidas por otros componentes (IA y Actuator), no por controladores. */
    private static final Set<String> NOT_CONTROLLERS = Set.of("GET /health", "POST /v1/analyze", "GET /actuator/health");

    /** Rutas del contrato aún sin implementar (vacío: el contrato está completo). */
    private static final Set<String> PENDING = Set.of();

    @Autowired MockMvc mvc;

    @Test
    void lasRutasDeSpringCoincidenConElContrato() throws Exception {
        String json = mvc.perform(get("/v3/api-docs")).andReturn().getResponse().getContentAsString();
        Set<String> spring = operations(new ObjectMapper().readTree(json).get("paths"));

        Set<String> contract = contractOperations();
        contract.removeAll(NOT_CONTROLLERS);

        assertThat(spring).as("Spring expone rutas que no están en el contrato").isSubsetOf(contract);
        Set<String> missing = new TreeSet<>(contract);
        missing.removeAll(spring);
        assertThat(missing).as("Rutas del contrato sin implementar").isEqualTo(new TreeSet<>(PENDING));
    }

    private static Set<String> operations(JsonNode paths) {
        Set<String> ops = new TreeSet<>();
        for (Iterator<Map.Entry<String, JsonNode>> it = paths.fields(); it.hasNext(); ) {
            Map.Entry<String, JsonNode> path = it.next();
            path.getValue().fieldNames().forEachRemaining(m -> ops.add(m.toUpperCase() + " " + normalize(path.getKey())));
        }
        return ops;
    }

    @SuppressWarnings("unchecked")
    private static Set<String> contractOperations() throws Exception {
        try (InputStream in = Files.newInputStream(Path.of("contracts", "tizon-api.yaml"))) {
            Map<String, Map<String, Object>> paths = (Map<String, Map<String, Object>>) new Yaml().<Map<String, Object>>load(in).get("paths");
            return paths.entrySet().stream()
                    .flatMap(p -> p.getValue().keySet().stream().map(m -> m.toUpperCase() + " " + normalize(p.getKey())))
                    .collect(Collectors.toCollection(TreeSet::new));
        }
    }

    /** {session_id} y {sessionId} se comparan como {}. */
    private static String normalize(String path) {
        return path.replaceAll("\\{[^}]+}", "{}");
    }
}
