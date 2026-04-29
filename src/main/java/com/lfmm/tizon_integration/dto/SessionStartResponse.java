package com.lfmm.tizon_integration.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SessionStartResponse(
        @JsonProperty("session_id") String sessionId,
        String status
) {}
