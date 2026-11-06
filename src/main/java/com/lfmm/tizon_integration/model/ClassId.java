package com.lfmm.tizon_integration.model;

/**
 * Clases del modelo (contrato: ClassId). Los textos visibles los resuelve la app.
 * El orden es el de severidad: decide el resultado principal y los desempates.
 * {@code affected}: la clase cuenta como afectada (AppConfig.classes[].affected y affection_pct).
 */
public enum ClassId {
    SANO(false),
    TIZON_TEMPRANO(true),
    TIZON_TARDIO(true);

    private final boolean affected;

    ClassId(boolean affected) {
        this.affected = affected;
    }

    public boolean affected() {
        return affected;
    }
}
