package com.spectre.publicapi;

import com.spectre.auth.DiscordOauthService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class ConfigController {

    private final DiscordOauthService discord;

    @Value("${app.discord.client-id:}")
    private String discordClientId;

    @GetMapping("/config")
    public Map<String, Object> config() {
        return Map.of(
                "oauthProvider", "discord",
                "discordClientId", discordClientId,
                "loginUrl", discord.getLoginUrl(),
                "swaggerUrl", "/swagger-ui.html",
                "features", Map.of(
                        "posts", true,
                        "images", true,
                        "ships", true,
                        "compare", true,
                        "commodities", true,
                        "routes", true
                )
        );
    }
}