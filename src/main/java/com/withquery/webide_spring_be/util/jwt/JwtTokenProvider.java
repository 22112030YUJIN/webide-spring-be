package com.withquery.webide_spring_be.util.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret:defaultSecretKeyForDevelopmentOnly}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400000}") // 24시간 (밀리초)
    private long jwtExpiration;

    @Value("${jwt.password-reset.expiration:300000}") // 5분 (밀리초)
    private long passwordResetExpiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String email, String nickname, Long id) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);

        return Jwts.builder()
                .subject(nickname)
                .claim("email", email)
                .claim("id", id)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    public String generatePasswordResetToken(String email) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + passwordResetExpiration);

        return Jwts.builder()
                .subject("password-reset")
                .claim("email", email)
                .claim("type", "password-reset")
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    public Long getIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        
        return claims.get("id", Long.class);
    }

    public String getNicknameFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getSubject();
    }

    public String getEmailFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.get("email", String.class);
    }

    public String getEmailFromPasswordResetToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        String type = claims.get("type", String.class);
        if (!"password-reset".equals(type)) {
            throw new IllegalArgumentException("Invalid token type for password reset");
        }

        return claims.get("email", String.class);
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    public boolean validatePasswordResetToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String type = claims.get("type", String.class);
            return "password-reset".equals(type);
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Invalid password reset JWT token: {}", e.getMessage());
            return false;
        }
    }
} 