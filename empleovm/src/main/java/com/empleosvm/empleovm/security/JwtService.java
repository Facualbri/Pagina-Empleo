package com.empleosvm.empleovm.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    // 30 días en milisegundos
    private static final long REFRESH_EXPIRATION_MS = 30L * 24 * 60 * 60 * 1000;

    private final SecureRandom secureRandom = new SecureRandom();

    private SecretKey getKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET demasiado corto. Mínimo 32 caracteres.");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /** Genera un access token (8 horas) */
    public String generarToken(Long userId, String email, String rol) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("rol", rol);

        return Jwts.builder()
                .claims(claims)
                .subject(email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getKey())
                .compact();
    }

    /**
     * Genera un refresh token opaco (64 bytes aleatorios, URL-safe base64).
     * No es un JWT — es solo un token seguro para buscar en la BD.
     */
    public String generarRefreshToken() {
        byte[] bytes = new byte[64];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** Tiempo de expiración del refresh token desde ahora */
    public long getRefreshExpirationMs() {
        return REFRESH_EXPIRATION_MS;
    }

    /** Extrae el email (subject) del token */
    public String extraerEmail(String token) {
        return parsear(token).getPayload().getSubject();
    }

    /** Extrae el userId del token */
    public Long extraerUserId(String token) {
        Object id = parsear(token).getPayload().get("userId");
        if (id instanceof Integer) return ((Integer) id).longValue();
        if (id instanceof Long)    return (Long) id;
        return Long.parseLong(id.toString());
    }

    /** Extrae el rol del token */
    public String extraerRol(String token) {
        return (String) parsear(token).getPayload().get("rol");
    }

    /** Valida que el token sea legítimo y corresponda al usuario */
    public boolean esValido(String token, UserDetails userDetails) {
        try {
            String email = extraerEmail(token);
            return email.equals(userDetails.getUsername()) && !estaExpirado(token);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Token JWT inválido: {}", e.getMessage());
            return false;
        }
    }

    private boolean estaExpirado(String token) {
        return parsear(token).getPayload().getExpiration().before(new Date());
    }

    private Jws<Claims> parsear(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token);
    }
}