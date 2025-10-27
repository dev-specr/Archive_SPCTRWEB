package com.spectre.sync;

import com.spectre.user.Role;
import com.spectre.user.User;
import com.spectre.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class GuildMembershipSyncService {

    private final UserRepository userRepository;
    private final RestTemplate rest;

    @Value("${app.discord.bot-token:}")
    private String botToken;

    @Value("${app.discord.guild-id}")
    private String guildId;

    
    @Scheduled(cron = "0 0 4 * * *")
    public void scheduledRecheck() {
        try {
            SyncResult r = recheckAllMembers();
            log.info("Guild recheck: kept={}, downgraded={}, errors={}", r.kept, r.downgraded, r.errors);
        } catch (Exception e) {
            log.error("Guild recheck failed: {}", e.getMessage(), e);
        }
    }

    public SyncResult recheckAllMembers() {
        if (botToken == null || botToken.isBlank()) {
            log.warn("No bot token configured; skipping guild recheck.");
            return new SyncResult(0,0,0);
        }
        List<User> candidates = userRepository.findAll(); 
        int kept = 0, downgraded = 0, errors = 0;

        for (User u : candidates) {
            if (u.getDiscordId() == null) continue;

            boolean wasMember = u.getRoles() != null && u.getRoles().contains(Role.ROLE_MEMBER);
            boolean isMemberNow;
            try {
                isMemberNow = checkMembership(u.getDiscordId());
            } catch (RateLimitedException rl) {
                
                try { Thread.sleep(rl.retryAfterMs()); } catch (InterruptedException ignored) {}
                
                try {
                    isMemberNow = checkMembership(u.getDiscordId());
                } catch (Exception e) {
                    log.warn("Recheck failed after retry for {}: {}", u.getDiscordId(), e.getMessage());
                    errors++; continue;
                }
            } catch (Exception e) {
                log.warn("Recheck error for {}: {}", u.getDiscordId(), e.getMessage());
                errors++; continue;
            }

            if (isMemberNow) {
                
                if (!wasMember) {
                    Set<Role> roles = u.getRoles() != null ? new HashSet<>(u.getRoles()) : new HashSet<>();
                    roles.add(Role.ROLE_USER);
                    roles.add(Role.ROLE_MEMBER);
                    u.setRoles(roles);
                    userRepository.save(u);
                }
                kept++;
            } else {
                
                if (wasMember) {
                    Set<Role> roles = u.getRoles() != null ? new HashSet<>(u.getRoles()) : new HashSet<>();
                    roles.remove(Role.ROLE_MEMBER);
                    
                    roles.add(Role.ROLE_USER);
                    u.setRoles(roles);
                    userRepository.save(u);
                    downgraded++;
                }
            }

            
            try { Thread.sleep(100); } catch (InterruptedException ignored) {}
        }

        return new SyncResult(kept, downgraded, errors);
    }

    private boolean checkMembership(String discordUserId) {
        String url = "https://discord.com/api/v10/guilds/" + guildId + "/members/" + discordUserId;

        HttpHeaders h = new HttpHeaders();
        h.set("Authorization", botToken.trim()); 
        h.setAccept(List.of(MediaType.APPLICATION_JSON));
        HttpEntity<Void> req = new HttpEntity<>(h);

        try {
            ResponseEntity<String> resp = rest.exchange(url, HttpMethod.GET, req, String.class);
            return resp.getStatusCode().is2xxSuccessful(); 
        } catch (HttpStatusCodeException ex) {
            if (ex.getStatusCode().value() == 404) return false;     
            if (ex.getStatusCode().value() == 429) {                  
                long retryMs = parseRetryAfterMs(ex);
                throw new RateLimitedException(retryMs);
            }
            
            throw ex;
        }
    }

    private long parseRetryAfterMs(HttpStatusCodeException ex) {
        try {
            String ra = ex.getResponseHeaders() != null ? ex.getResponseHeaders().getFirst("Retry-After") : null;
            if (ra == null) return 1000;
            
            double seconds = Double.parseDouble(ra);
            return (long) (seconds * 1000);
        } catch (Exception ignore) {
            return 1000;
        }
    }

    public record SyncResult(int kept, int downgraded, int errors) { }

    static class RateLimitedException extends RuntimeException {
        private final long retryAfterMs;
        RateLimitedException(long ms) { super("Rate limited, retry after " + ms + "ms"); this.retryAfterMs = ms; }
        long retryAfterMs() { return retryAfterMs; }
    }
}
