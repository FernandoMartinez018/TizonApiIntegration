package com.lfmm.tizon_integration.controller;

import com.lfmm.tizon_integration.dto.AlertResponse;
import com.lfmm.tizon_integration.model.Alert;
import com.lfmm.tizon_integration.service.AlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    /** {@code since} permite el sondeo incremental de la app (WorkManager cada 15 min). */
    @GetMapping
    public List<AlertResponse> list(
            @AuthenticationPrincipal UserDetails user,
            @RequestParam(required = false) Alert.Status status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant since) {
        return alertService.list(user, status, since);
    }

    @PostMapping("/{alertId}/ack")
    public AlertResponse acknowledge(@AuthenticationPrincipal UserDetails user, @PathVariable UUID alertId) {
        return alertService.acknowledge(user.getUsername(), alertId);
    }
}
