package com.spectre.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SecurityProps {
    @Value("${security.cors.allowed-origins:http://localhost:5173}")
    private String allowedOrigins;

    public String getAllowedOrigins() {
        return allowedOrigins;
    }
}