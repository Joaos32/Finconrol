package com.fincontrol.user.security;

import com.fincontrol.user.entity.UserEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public record AppUserPrincipal(UUID id, String email, String passwordHash) implements UserDetails {
    public static AppUserPrincipal from(UserEntity user) {
        return new AppUserPrincipal(user.getId(), user.getEmail(), user.getPasswordHash());
    }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }
    @Override public String getPassword() { return passwordHash; }
    @Override public String getUsername() { return email; }
}
