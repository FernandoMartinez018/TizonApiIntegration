package com.lfmm.tizon_integration.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PythonAnalyzeRequest(
        @JsonProperty("frame_b64")  String frameB64,
        @JsonProperty("session_id") String sessionId
) {}
