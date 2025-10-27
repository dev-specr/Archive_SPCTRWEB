package com.spectre.auth;

import com.spectre.user.Role;
import com.spectre.user.User;
import com.spectre.user.UserRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class DiscordOauthService {

    @Value("${app.discord.client-id}")
    private String clientId;
    @Value("${app.discord.client-secret}")
    private String clientSecret;
    @Value("${app.discord.redirect-uri}")
    private String redirectUri;
    @Value("${app.discord.guild-id}")
    private String guildId;

    @Value("${app.admin.discord-ids:}")
    private String adminIdsCsv;

    @Value("${app.discord.admin-role-ids:}")
    private String adminRoleIdsCsv;
    @Value("${app.discord.member-role-ids:}")
    private String memberRoleIdsCsv;
    @Value("${app.discord.bot-token:}")
    private String botToken;

    private final UserRepository userRepository;
    private final RestTemplate rest;
    private final AuthStateStore authStateStore;

    public String getLoginUrl() {
        String scope = url("identify guilds");
        String redirect = url(redirectUri);
        String state = authStateStore.issue();
        return "https://discord.com/api/oauth2/authorize"
                + "?response_type=code"
                + "&client_id=" + clientId
                + "&scope=" + scope
                + "&redirect_uri=" + redirect
                + "&state=" + state
                + "&prompt=consent";
    }

    public User handleCallbackExchange(String code) {

        TokenResponse token = exchangeCodeForToken(code);

        DiscordUser me = getMe(token.getAccessToken());

        DiscordGuild[] guilds = getMyGuilds(token.getAccessToken());

        boolean isMember = Arrays.stream(guilds)
                .anyMatch(g -> g.getId() != null && g.getId().equals(guildId));


        Optional<User> existing = userRepository.findByDiscordId(me.getId());
        User user = existing.orElseGet(User::new);

        user.setDiscordId(me.getId());
        user.setUsername(me.getUsername() + (me.getDiscriminator() != null && !me.getDiscriminator().isBlank()
                ? "#" + me.getDiscriminator() : ""));
        user.setAvatar(me.getAvatar());

        Set<Role> roles = (user.getRoles() != null) ? new HashSet<>(user.getRoles()) : new HashSet<>();

        roles.add(Role.ROLE_USER);
        boolean hasMemberDiscordRole = hasAnyRole(me.getId(), memberRoleIds());
        boolean hasAdminDiscordRole = hasAnyRole(me.getId(), adminRoleIds());
        if (isMember || hasMemberDiscordRole) roles.add(Role.ROLE_MEMBER); else roles.remove(Role.ROLE_MEMBER);
        if (isAdminSeed(me.getId()) || hasAdminDiscordRole) roles.add(Role.ROLE_ADMIN); else roles.remove(Role.ROLE_ADMIN);

        user.setRoles(roles);
        try {
            return userRepository.save(user);
        } catch (Exception e) {

            log.warn("User persistence failed, proceeding stateless. reason={}", e.getMessage());
            return user; 
        }
    }

    private boolean isAdminSeed(String discordUserId) {
        if (adminIdsCsv == null || adminIdsCsv.isBlank()) return false;
        for (String id : adminIdsCsv.split(",")) {
            if (discordUserId.equals(id.trim())) return true;
        }
        return false;
    }

    private boolean hasAnyRole(String discordUserId, java.util.Set<String> roleIds) {
        if (roleIds.isEmpty() || botToken == null || botToken.isBlank()) return false;
        try {
            java.util.Set<String> roles = getGuildMemberRoleIds(discordUserId);
            for (String r : roleIds) if (roles.contains(r)) return true;
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private java.util.Set<String> adminRoleIds() {
        java.util.Set<String> out = new java.util.HashSet<>();
        if (adminRoleIdsCsv == null || adminRoleIdsCsv.isBlank()) return out;
        for (String r : adminRoleIdsCsv.split(",")) out.add(r.trim());
        return out;
    }

    private java.util.Set<String> memberRoleIds() {
        java.util.Set<String> out = new java.util.HashSet<>();
        if (memberRoleIdsCsv == null || memberRoleIdsCsv.isBlank()) return out;
        for (String r : memberRoleIdsCsv.split(",")) out.add(r.trim());
        return out;
    }

    private java.util.Set<String> getGuildMemberRoleIds(String discordUserId) {
        java.util.Set<String> out = new java.util.HashSet<>();
        if (botToken == null || botToken.isBlank()) return out;
        HttpHeaders h = new HttpHeaders();
        h.set("Authorization", botToken.trim()); 
        ResponseEntity<GuildMember> r = rest.exchange(
                "https://discord.com/api/v10/guilds/" + guildId + "/members/" + discordUserId,
                HttpMethod.GET, new HttpEntity<>(h), GuildMember.class);
        if (r.getStatusCode().is2xxSuccessful() && r.getBody() != null && r.getBody().roles != null) {
            out.addAll(r.getBody().roles);
        }
        return out;
    }

    private TokenResponse exchangeCodeForToken(String code) {
        String url = "https://discord.com/api/oauth2/token";

        
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("code", code);
        form.add("redirect_uri", redirectUri);

        
        try {
            HttpHeaders h = new HttpHeaders();
            h.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            String basic = java.util.Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            h.set("Authorization", "Basic " + basic);
            ResponseEntity<TokenResponse> r = rest.postForEntity(url, new HttpEntity<>(form, h), TokenResponse.class);
            if (r.getStatusCode().is2xxSuccessful() && r.getBody() != null && r.getBody().getAccessToken() != null) {
                return r.getBody();
            }
            log.warn("Discord token exchange (basic) unexpected status={} bodyNull={}", r.getStatusCode().value(), r.getBody() == null);
        } catch (org.springframework.web.client.HttpStatusCodeException ex) {
            
            if (ex.getStatusCode().value() == 400 || ex.getStatusCode().value() == 401) {
                log.warn("Discord token basic auth failed ({}), will retry with form creds. body={}", ex.getStatusCode().value(), ex.getResponseBodyAsString());
            } else {
                log.warn("Discord token basic auth error {} body={}", ex.getStatusCode().value(), ex.getResponseBodyAsString());
            }
        } catch (Exception e) {
            log.warn("Discord token basic auth error: {}", e.getMessage());
        }

        
        try {
            HttpHeaders h = new HttpHeaders();
            h.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            MultiValueMap<String, String> withCreds = new LinkedMultiValueMap<>(form);
            withCreds.add("client_id", clientId);
            withCreds.add("client_secret", clientSecret);
            ResponseEntity<TokenResponse> r = rest.postForEntity(url, new HttpEntity<>(withCreds, h), TokenResponse.class);
            if (!r.getStatusCode().is2xxSuccessful() || r.getBody() == null || r.getBody().getAccessToken() == null) {
                log.warn("Discord token exchange (form) unexpected status={} bodyNull={}", r.getStatusCode().value(), r.getBody() == null);
                throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_GATEWAY, "Discord token exchange failed");
            }
            return r.getBody();
        } catch (org.springframework.web.client.HttpStatusCodeException ex) {
            log.warn("Discord token exchange (form) failed status={} body={}", ex.getStatusCode().value(), ex.getResponseBodyAsString());
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_GATEWAY,
                    "Discord OAuth error: " + ex.getStatusCode().value());
        } catch (Exception e) {
            log.warn("Discord token exchange (form) error: {}", e.getMessage());
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_GATEWAY, "Discord OAuth error");
        }
    }

    private DiscordUser getMe(String accessToken) {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(accessToken);
        try {
            ResponseEntity<DiscordUser> r = rest.exchange(
                    "https://discord.com/api/users/@me",
                    HttpMethod.GET, new HttpEntity<>(h), DiscordUser.class);
            if (!r.getStatusCode().is2xxSuccessful() || r.getBody() == null) {
                throw new RuntimeException("Failed to fetch Discord user");
            }
            return r.getBody();
        } catch (org.springframework.web.client.HttpStatusCodeException ex) {
            log.warn("Discord /users/@me failed status={} body={}", ex.getStatusCode().value(), ex.getResponseBodyAsString());
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_GATEWAY, "Discord identity fetch failed");
        }
    }

    private DiscordGuild[] getMyGuilds(String accessToken) {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(accessToken);
        try {
            ResponseEntity<DiscordGuild[]> r = rest.exchange(
                    "https://discord.com/api/users/@me/guilds",
                    HttpMethod.GET, new HttpEntity<>(h), DiscordGuild[].class);
            if (!r.getStatusCode().is2xxSuccessful() || r.getBody() == null) {
                throw new RuntimeException("Failed to fetch Discord guilds");
            }
            return r.getBody();
        } catch (org.springframework.web.client.HttpStatusCodeException ex) {
            log.warn("Discord /users/@me/guilds failed status={} body={}", ex.getStatusCode().value(), ex.getResponseBodyAsString());
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_GATEWAY, "Discord guilds fetch failed");
        }
    }

    private static String url(String v) {
        return URLEncoder.encode(v, StandardCharsets.UTF_8);
    }

    
    @Data
    public static class TokenResponse {
        private String access_token;
        private String token_type;
        private String refresh_token;
        private String scope;
        private Integer expires_in;

        public String getAccessToken() { return access_token; }
    }

    @Data
    public static class DiscordUser {
        private String id;
        private String username;
        private String discriminator; 
        private String avatar;
    }

    @Data
    public static class DiscordGuild {
        private String id;
        private String name;
        private String icon;
        private Boolean owner;
        private Integer permissions; 
    }

    @lombok.Data
    public static class GuildMember {
        private java.util.List<String> roles;
    }
}
