package com.fincontrol.auth.service;

import com.fincontrol.auth.dto.AuthDtos;
import com.fincontrol.category.entity.CategoryEntity;
import com.fincontrol.category.entity.CategoryType;
import com.fincontrol.category.repository.CategoryRepository;
import com.fincontrol.shared.error.ApiException;
import com.fincontrol.shared.security.JwtTokenService;
import com.fincontrol.user.entity.UserEntity;
import com.fincontrol.user.repository.UserRepository;
import com.fincontrol.user.security.AppUserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class AuthService {
    private static final List<String> EXPENSE_CATEGORIES = List.of(
            "Alimentação", "Moradia", "Transporte", "Saúde", "Educação",
            "Lazer", "Assinaturas", "Compras", "Outros");
    private static final List<String> INCOME_CATEGORIES = List.of(
            "Salário", "Freelance", "Investimentos", "Outros");

    private final UserRepository users;
    private final CategoryRepository categories;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenService tokens;

    public AuthService(UserRepository users, CategoryRepository categories, PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager, JwtTokenService tokens) {
        this.users = users;
        this.categories = categories;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokens = tokens;
    }

    @Transactional
    public AuthDtos.AuthResponse register(AuthDtos.RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (users.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("Este e-mail já está cadastrado.");
        }
        UserEntity user = users.save(new UserEntity(request.name().trim(), email,
                passwordEncoder.encode(request.password())));
        categories.saveAll(EXPENSE_CATEGORIES.stream()
                .map(name -> new CategoryEntity(user, name, CategoryType.EXPENSE)).toList());
        categories.saveAll(INCOME_CATEGORIES.stream()
                .map(name -> new CategoryEntity(user, name, CategoryType.INCOME)).toList());
        return response(user.getId());
    }

    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizeEmail(request.email()), request.password()));
            AppUserPrincipal principal = (AppUserPrincipal) authentication.getPrincipal();
            return response(principal.id());
        } catch (BadCredentialsException | UsernameNotFoundException exception) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos.");
        }
    }

    private AuthDtos.AuthResponse response(java.util.UUID userId) {
        return new AuthDtos.AuthResponse(tokens.createAccessToken(userId), "Bearer");
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
