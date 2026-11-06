package com.lfmm.tizon_integration.model;

/**
 * Resultados válidos, no errores: NO_VEGETATION (no hay hoja analizable) y NOT_POTATO (la hoja es de otra planta y
 * no se analiza el tizón).
 */
public enum FrameStatus {
    DETECTED,
    NO_VEGETATION,
    NOT_POTATO
}
