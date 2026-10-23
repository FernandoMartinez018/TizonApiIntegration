package com.lfmm.tizon_integration.config;

import io.netty.channel.ChannelOption;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class WebClientConfig {

    /**
     * Cliente hacia la IA. Usa el WebClient.Builder de Spring Boot para heredar el ObjectMapper
     * global (snake_case). El token interno va en todas las peticiones.
     */
    @Bean
    public WebClient iaWebClient(WebClient.Builder builder, IaProperties ia) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3_000)
                .responseTimeout(Duration.ofSeconds(ia.timeoutSeconds()));

        return builder
                .baseUrl(ia.baseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("X-Internal-Token", ia.internalToken())
                // Frames de hasta 2 MB en Base64 (~2,7 MB) más el JSON
                .codecs(config -> config.defaultCodecs().maxInMemorySize(4 * 1024 * 1024))
                .build();
    }
}
