package com.spectre.user;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MeController {

    private final UserRepository repo;

    @GetMapping("/me")
    public ResponseEntity<?> me(@AuthenticationPrincipal UserDetails principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "unauthorized"));
        }
        try {
            Long id = Long.parseLong(principal.getUsername());
            User user = repo.findById(id).orElseThrow();
            return ResponseEntity.ok(Map.of(
                    "id", user.getId(),
                    "username", user.getUsername(),
                    "roles", user.getRoles()
            ));
        } catch (NumberFormatException e) {
            
            var roles = principal.getAuthorities().stream().map(a -> a.getAuthority()).toList();
            return ResponseEntity.ok(Map.of(
                    "id", null,
                    "username", principal.getUsername(),
                    "roles", roles
            ));
        }
    }
}
