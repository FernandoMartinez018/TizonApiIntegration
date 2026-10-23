package com.lfmm.tizon_integration.dto;

/** Respuesta de GET /health de la IA (solo los campos que usa Spring). */
public record IaHealth(String status, boolean modelLoaded, String modelVersion) {}
