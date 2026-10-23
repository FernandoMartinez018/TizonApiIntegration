package com.lfmm.tizon_integration.controller;

import com.lfmm.tizon_integration.config.TizonProperties;
import com.lfmm.tizon_integration.dto.AppConfigResponse;
import com.lfmm.tizon_integration.dto.AppConfigResponse.ClassConfig;
import com.lfmm.tizon_integration.dto.AppConfigResponse.SeverityThresholds;
import com.lfmm.tizon_integration.model.ClassId;
import com.lfmm.tizon_integration.service.IaClient;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Clases, umbrales y ritmo de envío para la app: una sola fuente (RNF-MAN-03). */
@RestController
@RequiredArgsConstructor
public class ConfigController {

    private final TizonProperties props;
    private final IaClient iaClient;

    @GetMapping("/config")
    public AppConfigResponse config() {
        return new AppConfigResponse(
                List.of(new ClassConfig(ClassId.SANO, false), new ClassConfig(ClassId.TIZON_TARDIO, true)),
                new SeverityThresholds(props.severity().warningPct(), props.severity().criticalPct()),
                props.alertThresholdPct(),
                props.frameIntervalMs(),
                iaClient.modelVersion());
    }
}
