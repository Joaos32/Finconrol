package com.fincontrol.shared.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "fincontrol.jwt")
public record JwtProperties(String secret, Duration expiration) {
}
