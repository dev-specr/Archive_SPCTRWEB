package com.spectre.ship;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;

import java.util.List;

import com.spectre.ship.dto.ShipDtos;

@RestController
@RequestMapping(value = "/api/ships")
@RequiredArgsConstructor
public class ShipController {

    private final ShipService service;
    private final ShipSyncService syncService;

    
    @GetMapping("/names")
    @PreAuthorize("hasAnyRole('USER','MEMBER','ADMIN')")
    public List<String> names() {
        return service.names();
    }

    
    @GetMapping("/info")
    @PreAuthorize("hasAnyRole('USER','MEMBER','ADMIN')")
    public ShipDtos.ShipResponse info(@RequestParam("name") String name) {
        Ship s = service.infoByName(name);
        return ShipDtos.ShipResponse.builder()
                .id(s.getId())
                .uuid(s.getUuid())
                .name(s.getName())
                .manufacturer(s.getManufacturer())
                .type(s.getType())
                .focus(s.getFocus())
                .size(s.getSize())
                .cargoCapacity(s.getCargoCapacity())
                .scmSpeed(s.getScmSpeed())
                .navMaxSpeed(s.getNavMaxSpeed())
                .pitch(s.getPitch())
                .yaw(s.getYaw())
                .roll(s.getRoll())
                .hp(s.getHp())
                .pledgeUrl(s.getPledgeUrl())
                .description(s.getDescription())
                .build();
    }

    
    @PostMapping("/admin/sync")
    @PreAuthorize("hasRole('ADMIN')")
    public String manualSync() throws Exception {
        int n = syncService.syncAll();
        return "Synced " + n + " ships.";
    }

    
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('USER','MEMBER','ADMIN')")
    public java.util.List<String> search(@RequestParam("q") String q,
                                         @RequestParam(value = "limit", required = false) Integer limit) {
        return service.searchNames(q, limit);
    }
}
