package com.spectre.auth;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Component
public class AuthStateStore {
    private static final long TTL_MS = 10 * 60 * 1000; 
    private final Map<String, Long> states = new ConcurrentHashMap<>();
    private final SecureRandom rng = new SecureRandom();

    public String issue() {
        byte[] buf = new byte[24];
        rng.nextBytes(buf);
        String state = Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
        states.put(state, System.currentTimeMillis() + TTL_MS);
        return state;
    }

    public boolean consume(String state) {
        if (state == null) return false;
        Long exp = states.remove(state);
        return exp != null && exp >= System.currentTimeMillis();
    }

    public void clearExpired() {
        long now = System.currentTimeMillis();
        states.entrySet().removeIf(e -> e.getValue() < now);
    }
}

