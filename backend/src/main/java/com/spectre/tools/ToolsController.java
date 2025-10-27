package com.spectre.tools;

import com.spectre.ship.Ship;
import com.spectre.ship.ShipService;
import com.spectre.ship.dto.ShipDtos;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/tools")
@RequiredArgsConstructor
public class ToolsController {

    private final ShipService shipService;

    @PostMapping("/compare")
    @PreAuthorize("hasAnyRole('MEMBER','ADMIN')")
    public org.springframework.http.ResponseEntity<Map<String, Object>> compare(@RequestBody CompareRequest req) {
        
        Ship a = null; Ship b = null;
        java.util.List<String> aCandidates = java.util.List.of();
        java.util.List<String> bCandidates = java.util.List.of();

        try {
            a = shipService.infoByName(req.getA());
        } catch (org.springframework.web.server.ResponseStatusException notFound) {
            aCandidates = shipService.searchNames(req.getA(), 10);
        }
        try {
            b = shipService.infoByName(req.getB());
        } catch (org.springframework.web.server.ResponseStatusException notFound) {
            bCandidates = shipService.searchNames(req.getB(), 10);
        }

        if (a == null || b == null) {
            java.util.Map<String, Object> body = new java.util.HashMap<>();
            body.put("error", "Ambiguous or unknown ship name(s). Select from candidates.");
            if (!aCandidates.isEmpty()) body.put("aCandidates", aCandidates);
            if (!bCandidates.isEmpty()) body.put("bCandidates", bCandidates);
            return org.springframework.http.ResponseEntity.badRequest().body(body);
        }

        ShipDtos.ShipResponse aDto = ShipDtos.ShipResponse.builder()
                .id(a.getId()).uuid(a.getUuid()).name(a.getName()).manufacturer(a.getManufacturer())
                .type(a.getType()).focus(a.getFocus()).size(a.getSize())
                .cargoCapacity(a.getCargoCapacity()).scmSpeed(a.getScmSpeed()).navMaxSpeed(a.getNavMaxSpeed())
                .pitch(a.getPitch()).yaw(a.getYaw()).roll(a.getRoll()).hp(a.getHp())
                .pledgeUrl(a.getPledgeUrl()).description(a.getDescription())
                .build();
        ShipDtos.ShipResponse bDto = ShipDtos.ShipResponse.builder()
                .id(b.getId()).uuid(b.getUuid()).name(b.getName()).manufacturer(b.getManufacturer())
                .type(b.getType()).focus(b.getFocus()).size(b.getSize())
                .cargoCapacity(b.getCargoCapacity()).scmSpeed(b.getScmSpeed()).navMaxSpeed(b.getNavMaxSpeed())
                .pitch(b.getPitch()).yaw(b.getYaw()).roll(b.getRoll()).hp(b.getHp())
                .pledgeUrl(b.getPledgeUrl()).description(b.getDescription())
                .build();

        
        java.util.Map<String, Number> diff = new java.util.LinkedHashMap<>();
        diff.put("cargoCapacity", diff(a.getCargoCapacity(), b.getCargoCapacity()));
        diff.put("scmSpeed", diff(a.getScmSpeed(), b.getScmSpeed()));
        diff.put("navMaxSpeed", diff(a.getNavMaxSpeed(), b.getNavMaxSpeed()));
        diff.put("pitch", diff(a.getPitch(), b.getPitch()));
        diff.put("yaw", diff(a.getYaw(), b.getYaw()));
        diff.put("hp", diff(a.getHp(), b.getHp()));

        return org.springframework.http.ResponseEntity.ok(Map.of(
                "a", aDto,
                "b", bDto,
                "diff", diff
        ));
    }

    private Number diff(Number x, Number y) {
        if (x == null || y == null) return null;
        if (x instanceof Double || y instanceof Double) {
            return x.doubleValue() - y.doubleValue();
        }
        return x.intValue() - y.intValue();
    }

    @Data
    public static class CompareRequest {
        private String a;
        private String b;
    }
}
