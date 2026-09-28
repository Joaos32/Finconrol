package com.fincontrol.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {
    private AuthDtos() { }

    public record RegisterRequest(
            @NotBlank(message = "Informe seu nome.")
            @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres.")
            String name,
            @NotBlank(message = "Informe seu e-mail.")
            @Email(message = "Informe um e-mail válido.")
            @Size(max = 254, message = "O e-mail deve ter no máximo 254 caracteres.")
            String email,
            @NotBlank(message = "Informe sua senha.")
            @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres.")
            String password
    ) { }

    public record LoginRequest(
            @NotBlank(message = "Informe seu e-mail.")
            @Email(message = "Informe um e-mail válido.")
            String email,
            @NotBlank(message = "Informe sua senha.")
            String password
    ) { }

    public record AuthResponse(String accessToken, String tokenType) { }
}
