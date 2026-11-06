package com.lfmm.tizon_integration.security;

import com.lfmm.tizon_integration.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** Usuarios desde la base de datos (reemplaza al InMemoryUserDetailsManager con admin/admin). */
@Service
@RequiredArgsConstructor
public class JpaUserDetailsService implements UserDetailsService {

    private final AppUserRepository users;

    @Override
    public UserDetails loadUserByUsername(String username) {
        return users.findByUsername(username)
                .map(u -> User.withUsername(u.getUsername())
                        .password(u.getPasswordHash())
                        .roles(u.getRole().name())
                        .disabled(!u.isEnabled())
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
    }
}
