package com.nexdrive.userService.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        Resource privateKeyPath,
        Resource publicKeyPath,
        long accessTokenExpirationMs,
        long refreshTokenExpirationMs,
        String issuer
) {}