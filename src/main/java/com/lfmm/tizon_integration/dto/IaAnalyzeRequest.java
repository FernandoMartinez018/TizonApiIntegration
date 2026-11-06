package com.lfmm.tizon_integration.dto;

/** Petición interna Spring → IA (POST /v1/analyze). */
public record IaAnalyzeRequest(String frameB64, String requestId) {}
