package com.spectre.auth;

import com.spectre.security.jwt.*;
import com.spectre.user.User;
import com.spectre.user.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final DiscordOauthService discordOauthService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;
    private final AuthStateStore authStateStore;

    @Value("${app.security.cookie-secure:false}")
    private boolean cookieSecure;
    @Value("${app.security.cookie-samesite:Lax}")
    private String cookieSameSite;

    @GetMapping("/discord/login")
    public ResponseEntity<Map<String,String>> loginUrl() {
        
        return ResponseEntity.ok(Map.of("url", discordOauthService.getLoginUrl()));
    }

    @GetMapping("/discord/callback")
    public ResponseEntity<?> callback(@RequestParam("code") String code,
                                      @RequestParam(value = "state", required = false) String state) {

        if (state == null || !authStateStore.consume(state)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid state"));
        }
        User user = discordOauthService.handleCallbackExchange(code);
        String access = jwtService.generateAccessToken(user);

        if (user.getId() != null) {
            refreshTokenService.deleteForUser(user.getId());
        }

        return ResponseEntity.ok(Map.of("accessToken", access));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "refresh disabled"));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestParam Long userId) {
        refreshTokenService.deleteForUser(userId);
        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(0)
                .sameSite(cookieSameSite)
                .build();
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie.toString()).build();
    }
}
