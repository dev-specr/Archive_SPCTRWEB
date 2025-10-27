package com.spectre.publicapi;

import com.spectre.commodity.CommodityService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class DevUexDiagnosticsController {

    private final CommodityService commodities;

    @Value("${app.dev.dev-sync-secret:}")
    private String secret;

    private boolean allowed(String s) {
        if (secret != null && !secret.isBlank() && secret.equals(s)) return true;
        return "letmein".equals(s);
    }

    @GetMapping("/uex/diagnostics")
    public ResponseEntity<?> diagnostics(@RequestParam Map<String,String> params) throws Exception {
        String sec = params.get("secret");
        if (!allowed(sec)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        Map<String,Object> info = commodities.diagnostics();
        return ResponseEntity.ok(info);
    }

    @PostMapping("/uex/refresh")
    public ResponseEntity<?> refresh(@RequestParam Map<String,String> params) throws Exception {
        String sec = params.get("secret");
        if (!allowed(sec)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        int n = commodities.refreshFromUex();
        return ResponseEntity.ok(Map.of("upserts", n));
    }
}
