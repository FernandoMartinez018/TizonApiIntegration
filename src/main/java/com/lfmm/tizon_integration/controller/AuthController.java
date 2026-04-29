package com.lfmm.tizon_integration.controller;

import com.lfmm.tizon_integration.dto.LoginRequest;
import com.lfmm.tizon_integration.dto.LoginResponse;
import com.lfmm.tizon_integration.security.JwtTokenProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthenticationManager authManager;
    private final JwtTokenProvider tokenProvider;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        log.info("Intento de login para usuario: {}", request.username());

        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.username(),
                        request.password()
                )
        );

        String token = tokenProvider.generateToken(auth.getName());
        log.info("Login exitoso para usuario: {}", auth.getName());

        return ResponseEntity.ok(new LoginResponse(
                token,
                auth.getName(),
                86_400_000L   // 24 horas en ms
        ));
        // Los errores de autenticación los maneja GlobalExceptionHandler
        // (BadCredentialsException → 401)
    }
}
