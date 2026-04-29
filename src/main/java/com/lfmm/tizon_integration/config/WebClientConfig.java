package com.lfmm.tizon_integration.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class WebClientConfig {

    @Value("${python.service.base-url}")
    private String pythonBaseUrl;

    @Value("${python.service.timeout-seconds:30}")
    private int timeoutSeconds;

    @Bean
    public WebClient pythonWebClient() {
        // Cliente HTTP Netty con timeouts a nivel de conexión
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000)  // 10s para conectar
                .responseTimeout(Duration.ofSeconds(timeoutSeconds))
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(timeoutSeconds, TimeUnit.SECONDS))
                                .addHandlerLast(new WriteTimeoutHandler(10, TimeUnit.SECONDS))
                );

        return WebClient.builder()
                .baseUrl(pythonBaseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .codecs(config ->
                        // 10 MB de buffer para frames de imagen en Base64
                        config.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)
                )
                .filter(logRequest())
                .filter(logResponse())
                .build();
    }

    // ── Filtros de logging ────────────────────────────────────────────────

    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(request -> {
            log.debug("→ Python | {} {}", request.method(), request.url());
            return Mono.just(request);
        });
    }

    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(response -> {
            log.debug("← Python | HTTP {}", response.statusCode());
            return Mono.just(response);
        });
    }
}
