package com.lfmm.tizon_integration.service;

import com.lfmm.tizon_integration.config.IaProperties;
import com.lfmm.tizon_integration.dto.ErrorCode;
import com.lfmm.tizon_integration.dto.IaAnalyzeRequest;
import com.lfmm.tizon_integration.dto.IaAnalyzeResponse;
import com.lfmm.tizon_integration.dto.IaHealth;
import com.lfmm.tizon_integration.exception.ApiException;
import io.netty.handler.timeout.ReadTimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.net.ConnectException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

/**
 * Cliente del servicio de IA (POST /v1/analyze). Sin fallback: cualquier falla técnica se
 * propaga como 400/502/504 y nunca se convierte en un resultado de diagnóstico (RF-07).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class IaClient {

    private static final String UNKNOWN_VERSION = "desconocido";

    private final WebClient iaWebClient;
    private final IaProperties ia;

    /** Última versión de modelo informada por la IA (para GET /config). */
    private volatile String lastModelVersion;

    public IaAnalyzeResponse analyze(String frameB64, String requestId) {
        IaAnalyzeResponse response = iaWebClient.post()
                .uri("/v1/analyze")
                .headers(h -> { if (requestId != null) h.set("X-Request-Id", requestId); })
                .bodyValue(new IaAnalyzeRequest(frameB64, requestId))
                .retrieve()
                .onStatus(s -> s.value() == 400 || s.value() == 422, res -> Mono.error(new ApiException(
                        HttpStatus.BAD_REQUEST, ErrorCode.INVALID_IMAGE, "La imagen enviada no es válida.")))
                .onStatus(s -> s.isError(), res -> {
                    log.error("La IA respondió HTTP {} (request_id={})", res.statusCode().value(), requestId);
                    return Mono.error(unavailable());
                })
                .bodyToMono(IaAnalyzeResponse.class)
                // Un solo reintento y solo si no se pudo conectar (la IA no alcanzó a procesar nada).
                .retryWhen(Retry.max(1).filter(IaClient::isConnectionRefused))
                .timeout(Duration.ofSeconds(ia.timeoutSeconds()))
                .onErrorMap(IaClient::isTimeout, ex -> new ApiException(
                        HttpStatus.GATEWAY_TIMEOUT, ErrorCode.IA_TIMEOUT,
                        "El análisis tardó demasiado. Intenta de nuevo."))
                .onErrorMap(WebClientRequestException.class, ex -> {
                    log.error("No se pudo contactar la IA (request_id={}): {}", requestId, ex.getMessage());
                    return unavailable();
                })
                .block();

        if (response == null) throw unavailable();
        lastModelVersion = response.modelVersion();
        return response;
    }

    public IaHealth health() {
        return iaWebClient.get().uri("/health").retrieve()
                .bodyToMono(IaHealth.class)
                .timeout(Duration.ofSeconds(3))
                .block();
    }

    public String modelVersion() {
        if (lastModelVersion != null) return lastModelVersion;
        try {
            IaHealth h = health();
            if (h != null && h.modelLoaded()) lastModelVersion = h.modelVersion();
        } catch (RuntimeException ex) {
            log.warn("No se pudo consultar la versión del modelo: {}", ex.getMessage());
        }
        return lastModelVersion != null ? lastModelVersion : UNKNOWN_VERSION;
    }

    private static ApiException unavailable() {
        return new ApiException(HttpStatus.BAD_GATEWAY, ErrorCode.IA_UNAVAILABLE,
                "El servicio de análisis no está disponible. Reintentando…");
    }

    private static boolean isConnectionRefused(Throwable ex) {
        return ex instanceof WebClientRequestException && ex.getCause() instanceof ConnectException;
    }

    private static boolean isTimeout(Throwable ex) {
        return ex instanceof TimeoutException
                || (ex instanceof WebClientRequestException && ex.getCause() instanceof ReadTimeoutException);
    }
}
