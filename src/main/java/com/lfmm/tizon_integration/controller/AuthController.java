package com.lfmm.tizon_integration.controller;

import com.lfmm.tizon_integration.dto.ErrorCode;
import com.lfmm.tizon_integration.dto.LoginRequest;
import com.lfmm.tizon_integration.dto.LoginResponse;
import com.lfmm.tizon_integration.dto.RegisterRequest;
import com.lfmm.tizon_integration.dto.UserResponse;
import com.lfmm.tizon_integration.exception.ApiException;
import com.lfmm.tizon_integration.security.JwtTokenProvider;
import com.lfmm.tizon_integration.security.LoginAttemptService;
import com.lfmm.tizon_integration.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthenticationManager authManager;
    private final JwtTokenProvider tokenProvider;
    private final UserService userService;
    private final LoginAttemptService loginAttempts;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.register(request));
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        if (loginAttempts.isBlocked(request.username())) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, ErrorCode.TOO_MANY_ATTEMPTS,
                    "Demasiados intentos fallidos. Espera 5 minutos e intenta de nuevo.");
        }
        Authentication auth;
        try {
            auth = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        } catch (AuthenticationException ex) {
            loginAttempts.onFailure(request.username());
            log.warn("Login fallido username={}", request.username());
            throw ex;   // → 401 BAD_CREDENTIALS sin decir qué falló (GlobalExceptionHandler)
        }
        loginAttempts.onSuccess(auth.getName());

        String token = tokenProvider.generateToken(auth.getName());
        log.info("Login exitoso username={}", auth.getName());
        return new LoginResponse(token, tokenProvider.getExpiration(token), userService.me(auth.getName()));
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal UserDetails user) {
        return userService.me(user.getUsername());
    }
}
