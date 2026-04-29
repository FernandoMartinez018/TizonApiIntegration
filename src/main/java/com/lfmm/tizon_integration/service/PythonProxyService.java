package com.lfmm.tizon_integration.service;

import com.lfmm.tizon_integration.dto.PythonAnalyzeRequest;
import com.lfmm.tizon_integration.dto.PythonAnalyzeResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class PythonProxyService {

    private final WebClient pythonWebClient;

    @Value("${python.service.timeout-seconds:30}")
    private int timeoutSeconds;

    public PythonAnalyzeResponse analyze(String frameB64, String sessionId) {
        log.debug("Enviando frame a Python. sessionId={}", sessionId);

        PythonAnalyzeRequest request = new PythonAnalyzeRequest(frameB64, sessionId);

        try {
            PythonAnalyzeResponse response = pythonWebClient
                    .post()
                    .uri("/analyze")
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(
                            status -> status.is4xxClientError(),
                            res -> {
                                log.error("Python rechazó el request: HTTP {}", res.statusCode());
                                return Mono.error(new IllegalArgumentException(
                                        "El servicio Python rechazó el frame enviado."));
                            }
                    )
                    .onStatus(
                            status -> status.is5xxServerError(),
                            res -> {
                                log.error("Error en el servicio Python: HTTP {}", res.statusCode());
                                return Mono.error(new RuntimeException(
                                        "Error interno en el servicio Python."));
                            }
                    )
                    .bodyToMono(PythonAnalyzeResponse.class)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .onErrorResume(ex -> {
                        // Ante cualquier error, retornar respuesta vacía en lugar de fallar
                        log.error("Error llamando a Python (fallback a vacío): {}", ex.getMessage());
                        return Mono.just(PythonAnalyzeResponse.empty());
                    })
                    .block();

            return response != null ? response : PythonAnalyzeResponse.empty();

        } catch (Exception ex) {
            log.error("Fallo inesperado en el proxy de Python: {}", ex.getMessage(), ex);
            return PythonAnalyzeResponse.empty();
        }
    }

    /**
     * Verifica que el servicio Python esté disponible.
     * Usa el endpoint /health de FastAPI.
     *
     * @return true si Python responde correctamente
     */
    public boolean isPythonHealthy() {
        try {
            var status = pythonWebClient
                    .get()
                    .uri("/health")
                    .retrieve()
                    .toBodilessEntity()
                    .timeout(Duration.ofSeconds(5))
                    .map(res -> res.getStatusCode() == HttpStatus.OK)
                    .onErrorReturn(false)
                    .block();
            return Boolean.TRUE.equals(status);
        } catch (Exception ex) {
            log.warn("Python health check falló: {}", ex.getMessage());
            return false;
        }
    }
}
