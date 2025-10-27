package com.spectre.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    private final RefreshTokenRepository repo;

    public RefreshToken createForUser(Long userId, long ttlMs) {
        RefreshToken token = RefreshToken.builder()
                .userId(userId)
                .token(UUID.randomUUID().toString())
                .expiry(Instant.now().plusMillis(ttlMs))
                .build();
        return repo.save(token);
    }

    public RefreshToken validate(String token) {
        RefreshToken rt = repo.findByToken(token).orElseThrow(() -> new RuntimeException("Invalid refresh token"));
        if (rt.getExpiry().isBefore(Instant.now())) {
            repo.delete(rt);
            throw new RuntimeException("Refresh token expired");
        }
        return rt;
    }

    public void deleteForUser(Long userId) {
        repo.deleteByUserId(userId);
    }
}