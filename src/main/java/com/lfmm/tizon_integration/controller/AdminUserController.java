package com.lfmm.tizon_integration.controller;

import com.lfmm.tizon_integration.dto.UpdateUserRequest;
import com.lfmm.tizon_integration.dto.UserResponse;
import com.lfmm.tizon_integration.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** Gestión de usuarios. Solo ADMIN (regla en SecurityConfig: /admin/** → 403 para otros roles). */
@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    public List<UserResponse> list() {
        return userService.list();
    }

    @PatchMapping("/{userId}")
    public UserResponse update(@AuthenticationPrincipal UserDetails admin,
                               @PathVariable UUID userId,
                               @RequestBody UpdateUserRequest request) {
        return userService.update(admin.getUsername(), userId, request);
    }
}
