package com.fincontrol.auth.service;

import com.fincontrol.auth.dto.AuthDtos;
import com.fincontrol.category.entity.CategoryEntity;
import com.fincontrol.category.repository.CategoryRepository;
import com.fincontrol.shared.error.ApiException;
import com.fincontrol.shared.security.JwtTokenService;
import com.fincontrol.user.entity.UserEntity;
import com.fincontrol.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock private UserRepository users;
    @Mock private CategoryRepository categories;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtTokenService tokens;
    @InjectMocks private AuthService service;

    @Test
    void registrationHashesThePasswordAndCreatesDefaultCategories() {
        UUID userId = UUID.randomUUID();
        UserEntity savedUser = mock(UserEntity.class);
        when(users.existsByEmailIgnoreCase("joao@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Senha123!")).thenReturn("bcrypt-hash");
        when(users.save(any(UserEntity.class))).thenReturn(savedUser);
        when(savedUser.getId()).thenReturn(userId);
        when(tokens.createAccessToken(userId)).thenReturn("signed-token");

        AuthDtos.AuthResponse response = service.register(
                new AuthDtos.RegisterRequest("João Silva", "Joao@Example.com", "Senha123!"));

        assertEquals("signed-token", response.accessToken());
        assertEquals("Bearer", response.tokenType());
        verify(users).save(org.mockito.ArgumentMatchers.argThat(user ->
                user.getEmail().equals("joao@example.com") && user.getPasswordHash().equals("bcrypt-hash")));
        verify(categories, org.mockito.Mockito.times(2)).saveAll(any());
        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    void rejectsDuplicateEmailBeforeEncodingPassword() {
        when(users.existsByEmailIgnoreCase("joao@example.com")).thenReturn(true);

        ApiException exception = assertThrows(ApiException.class, () -> service.register(
                new AuthDtos.RegisterRequest("João Silva", "joao@example.com", "Senha123!")));

        assertEquals(409, exception.getStatus().value());
        verify(passwordEncoder, never()).encode(any());
        verify(categories, never()).saveAll(any());
    }
}
