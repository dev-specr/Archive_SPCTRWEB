package com.spectre.security.jwt;

import com.spectre.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;
import jakarta.annotation.PostConstruct;

@Service
public class JwtService {

    @Value("${app.jwt.secret}")
    private String secret;
    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;
    @Value("${app.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    private Key signingKey;

    @PostConstruct
    void init() {
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secret);
        } catch (Exception e) {
            throw new IllegalStateException("Invalid JWT secret: must be Base64-encoded", e);
        }
        if (keyBytes.length < 32) { 
            throw new IllegalStateException("JWT secret too short: require >= 256 bits (Base64) for HS256");
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    private Key getSigningKey() { return signingKey; }

    public String generateAccessToken(User user) {
        String subject;
        if (user.getId() != null) {
            subject = String.valueOf(user.getId());
        } else if (user.getDiscordId() != null) {
            subject = "d:" + user.getDiscordId();
        } else {
            subject = "anon"; 
        }

        return Jwts.builder()
                .setSubject(subject)
                .addClaims(Map.of(
                        "username", user.getUsername(),
                        "roles", user.getRoles(),
                        "discordId", user.getDiscordId()
                ))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateRefreshToken(User user) {
        return Jwts.builder()
                .setSubject(String.valueOf(user.getId()))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + refreshExpirationMs))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractSubject(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claimsResolver.apply(claims);
    }

    public boolean isTokenValid(String token, User user) {
        String subject = extractSubject(token);
        Date exp = extractClaim(token, Claims::getExpiration);
        if (!exp.toInstant().isAfter(Instant.now())) return false;
        if (user.getId() != null) {
            return subject.equals(String.valueOf(user.getId()));
        }
        if (user.getDiscordId() != null) {
            return subject.equals("d:" + user.getDiscordId());
        }
        return false;
    }
}
