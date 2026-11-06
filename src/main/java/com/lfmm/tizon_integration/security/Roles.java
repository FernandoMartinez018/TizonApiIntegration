package com.lfmm.tizon_integration.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/** Roles del contrato: AGRICULTOR (por defecto), TECNICO y ADMIN. */
public final class Roles {

    public static final String AGRICULTOR = "AGRICULTOR";
    public static final String TECNICO = "TECNICO";
    public static final String ADMIN = "ADMIN";

    private Roles() {}

    /** TECNICO y ADMIN pueden leer sesiones y alertas de todos los usuarios. */
    public static boolean isStaff(UserDetails user) {
        return has(user, ADMIN) || has(user, TECNICO);
    }

    public static String primaryRole(UserDetails user) {
        if (has(user, ADMIN)) return ADMIN;
        if (has(user, TECNICO)) return TECNICO;
        return AGRICULTOR;
    }

    private static boolean has(UserDetails user, String role) {
        String authority = "ROLE_" + role;
        return user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority::equals);
    }
}
