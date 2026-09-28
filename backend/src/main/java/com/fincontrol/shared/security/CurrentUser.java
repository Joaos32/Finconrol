package com.fincontrol.shared.security;

import com.fincontrol.shared.error.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CurrentUser {
    public UUID id() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            try {
                return UUID.fromString(jwtAuthentication.getToken().getSubject());
            } catch (IllegalArgumentException ignored) {
                throw new ApiException(HttpStatus.UNAUTHORIZED, "Token inválido.");
            }
        }
        throw new ApiException(HttpStatus.UNAUTHORIZED, "Autenticação necessária.");
    }
}
