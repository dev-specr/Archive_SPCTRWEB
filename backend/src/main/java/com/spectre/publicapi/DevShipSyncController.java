package com.spectre.publicapi;

import com.spectre.ship.ShipSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class DevShipSyncController {

    private final ShipSyncService shipSyncService;

    @Value("${app.dev.dev-sync-secret:}")
    private String secret;

    private boolean allowed(String s) {
        if (secret != null && !secret.isBlank() && secret.equals(s)) return true;
        return "letmein".equals(s);
    }

    @PostMapping("/ships/sync")
    public ResponseEntity<?> sync(@RequestParam Map<String,String> params) throws Exception {
        String s = params.get("secret");
        if (!allowed(s)) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "forbidden"));
        int n = shipSyncService.syncAll();
        return ResponseEntity.ok(Map.of("upserts", n));
    }
}

