package com.lfmm.tizon_integration.controller;

import com.lfmm.tizon_integration.model.AppUser;
import com.lfmm.tizon_integration.model.AppUser.Role;
import com.lfmm.tizon_integration.repository.AppUserRepository;
import com.lfmm.tizon_integration.support.TestUsers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** F6-API-01 / RF-01, RF-02, RF-03: registro, login, deshabilitación y autorización por rol. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestUsers.class)
class UserFlowTest {

    @Autowired MockMvc mvc;
    @Autowired TestUsers testUsers;
    @Autowired AppUserRepository users;

    private ResultActions register(String username, String password) throws Exception {
        return mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"username":"%s","password":"%s","full_name":"Ana Pérez"}""".formatted(username, password)));
    }

    private ResultActions login(String username, String password) throws Exception {
        return mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                {"username":"%s","password":"%s"}""".formatted(username, password)));
    }

    private static String unique() {
        return "ana." + UUID.randomUUID().toString().substring(0, 8);
    }

    @Test
    void registroCreaAgricultorQuePuedeIniciarSesion() throws Exception {
        String username = unique();
        register(username, "clave-segura-1")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("AGRICULTOR"))
                .andExpect(jsonPath("$.full_name").value("Ana Pérez"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.password_hash").doesNotExist());

        login(username, "clave-segura-1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.expires_at").isNotEmpty())
                .andExpect(jsonPath("$.user.username").value(username));

        assertThat(users.findByUsername(username).orElseThrow().getPasswordHash()).startsWith("$2a$12$");
    }

    @Test
    void usuarioDuplicadoResponde409() throws Exception {
        String username = unique();
        register(username, "clave-segura-1").andExpect(status().isCreated());
        register(username, "otra-clave-22").andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USERNAME_TAKEN"));
    }

    @Test
    void claveCortaRespondeErrorEnElCampo() throws Exception {
        register(unique(), "corta").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fields.password").isNotEmpty());
    }

    @Test
    void credencialesInvalidasYUsuarioDeshabilitadoDanElMismo401() throws Exception {
        AppUser user = testUsers.create(Role.AGRICULTOR);
        login(user.getUsername(), "incorrecta").andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("BAD_CREDENTIALS"));

        user.setEnabled(false);
        users.save(user);
        login(user.getUsername(), TestUsers.PASSWORD).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("BAD_CREDENTIALS"));
    }

    @Test
    void tokenDeUsuarioDeshabilitadoDejaDeFuncionar() throws Exception {
        AppUser user = testUsers.create(Role.AGRICULTOR);
        String bearer = testUsers.bearer(user);
        mvc.perform(get("/auth/me").header("Authorization", bearer)).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId().toString()));

        user.setEnabled(false);
        users.save(user);
        mvc.perform(get("/auth/me").header("Authorization", bearer)).andExpect(status().isUnauthorized());
    }

    @Test
    void soloAdminGestionaUsuarios() throws Exception {
        AppUser agricultor = testUsers.create(Role.AGRICULTOR);
        AppUser tecnico = testUsers.create(Role.TECNICO);
        AppUser admin = testUsers.create(Role.ADMIN);

        mvc.perform(get("/admin/users").header("Authorization", testUsers.bearer(agricultor)))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));
        mvc.perform(get("/admin/users").header("Authorization", testUsers.bearer(tecnico)))
                .andExpect(status().isForbidden());

        mvc.perform(get("/admin/users").header("Authorization", testUsers.bearer(admin)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[*].username").isArray());

        mvc.perform(patch("/admin/users/" + agricultor.getId()).header("Authorization", testUsers.bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"TECNICO\",\"enabled\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("TECNICO"))
                .andExpect(jsonPath("$.enabled").value(false));
    }

    @Test
    void adminNoPuedeModificarseASiMismo() throws Exception {
        AppUser admin = testUsers.create(Role.ADMIN);
        mvc.perform(patch("/admin/users/" + admin.getId()).header("Authorization", testUsers.bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"AGRICULTOR\"}"))
                .andExpect(status().isConflict());
        assertThat(users.findById(admin.getId()).orElseThrow().getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    void cincoFallosBloqueanElUsuario() throws Exception {
        AppUser user = testUsers.create(Role.AGRICULTOR);
        for (int i = 0; i < 5; i++) login(user.getUsername(), "incorrecta").andExpect(status().isUnauthorized());

        login(user.getUsername(), TestUsers.PASSWORD).andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("TOO_MANY_ATTEMPTS"));
    }

    @Test
    void elAdminInicialSeCreaDesdeLaConfiguracion() {
        AppUser admin = users.findByUsername("admin").orElseThrow();
        assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
        assertThat(admin.getPasswordHash()).doesNotContain("admin-test-pass");
    }
}
