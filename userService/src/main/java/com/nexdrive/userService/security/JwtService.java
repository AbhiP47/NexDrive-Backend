package com.nexdrive.userService.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class JwtService {

    private final RSAPrivateKey privateKey;
    private final RSAPublicKey publicKey;
    private final JwtProperties jwtProperties;

    public JwtService(RSAPrivateKey privateKey, RSAPublicKey publicKey, JwtProperties jwtProperties) {
        this.privateKey = privateKey;
        this.publicKey = publicKey;
        this.jwtProperties = jwtProperties;
    }

    public String generateAccessToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));
        claims.put("type", "access");
        return buildToken(claims, userDetails.getUsername(), jwtProperties.accessTokenExpirationMs());
    }

    public String generateRefreshToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");
        return buildToken(claims, userDetails.getUsername(), jwtProperties.refreshTokenExpirationMs());
    }

    private String buildToken(Map<String, Object> claims, String subject, long expirationMs) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuer(jwtProperties.issuer())
                .issuedAt(now)
                .expiration(expiry)
                .id(UUID.randomUUID().toString()) // jti — useful for blacklisting/rotation
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    /** Parses & verifies signature + expiration. Throws JwtException subclasses on failure. */
    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public String extractTokenType(String token) {
        return extractAllClaims(token).get("type", String.class);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            Claims claims = extractAllClaims(token);
            boolean usernameMatches = claims.getSubject().equals(userDetails.getUsername());
            boolean notExpired = claims.getExpiration().after(new Date());
            return usernameMatches && notExpired;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}