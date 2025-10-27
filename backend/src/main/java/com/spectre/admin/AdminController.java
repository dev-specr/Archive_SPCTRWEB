package com.spectre.admin;

import com.spectre.commodity.CommodityService;
import com.spectre.user.Role;
import com.spectre.commodity.UexCommodityService;
import com.spectre.user.User;
import com.spectre.user.UserRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;


@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepo;
    private final CommodityService commodityService;
    private final UexCommodityService uexCommodityService;

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserSummary> listUsers() {
        return userRepo.findAll().stream().map(u -> {
            UserSummary s = new UserSummary();
            s.setId(u.getId());
            s.setDiscordId(u.getDiscordId());
            s.setUsername(u.getUsername());
            s.setRoles(u.getRoles() == null ? List.of() : u.getRoles().stream().map(Enum::name).sorted().toList());
            return s;
        }).toList();
    }

    @PostMapping("/users/{id}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public UserSummary setRoles(@PathVariable Long id, @RequestBody SetRoles req) {
        User u = userRepo.findById(id).orElseThrow();
        Set<Role> newRoles = new HashSet<>();
        for (String r : req.getRoles()) {
            try {
                newRoles.add(Role.valueOf(r));
            } catch (IllegalArgumentException ignore) {}
        }
        
        newRoles.add(Role.ROLE_USER);
        u.setRoles(newRoles);
        userRepo.save(u);
        UserSummary s = new UserSummary();
        s.setId(u.getId());
        s.setDiscordId(u.getDiscordId());
        s.setUsername(u.getUsername());
        s.setRoles(u.getRoles().stream().map(Enum::name).sorted().toList());
        return s;
    }

    @PostMapping("/commodities/refresh")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> refreshCommodities() throws Exception {
        int n = commodityService.refreshFromUex();
        return Map.of("upserts", n);
    }

    @GetMapping("/commodities/test")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> testCommodity(@RequestParam(name = "id", defaultValue = "1") long id) throws Exception {
        return commodityService.testCommodity(id);
    }

    @PostMapping("/commodities/catalog/refresh")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String,Object> refreshCatalog() throws Exception {
        int n = uexCommodityService.refreshCatalog();
        return Map.of("upserts", n);
    }

    @GetMapping("/commodities/diagnostics")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> commoditiesDiagnostics() throws Exception {
        return commodityService.diagnostics();
    }

    @Data
    public static class SetRoles {
        private List<String> roles;
    }

    @Data
    public static class UserSummary {
        private Long id;
        private String discordId;
        private String username;
        private List<String> roles;
    }
}
