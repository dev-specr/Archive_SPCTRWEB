package com.spectre.commodity;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/tools")
@RequiredArgsConstructor
public class CommodityController {

    private final CommodityService service;

    
    @GetMapping("/commodities")
    @PreAuthorize("hasAnyRole('MEMBER','ADMIN')")
    public List<CommodityService.CommoditySummary> commodities(@RequestParam(name = "q", required = false) String q) {
        return service.summarize(q);
    }

    
    @PostMapping("/routes")
    @PreAuthorize("hasAnyRole('MEMBER','ADMIN')")
    public List<CommodityService.RouteProposal> routes(@RequestBody RouteRequest req) {
        return service.bestRoutes(req.topN, req.quantity, req.system, req.allowCrossSystem != null && req.allowCrossSystem);
    }

    
    @GetMapping("/commodities/best")
    @PreAuthorize("hasAnyRole('MEMBER','ADMIN')")
    public Object best(@RequestParam(name = "commodityId", required = false) Long commodityId,
                       @RequestParam(name = "name", required = false) String name) {
        var best = service.bestFromDb(commodityId, name);
        if (best == null) return java.util.Map.of("error", "Not found");
        return best;
    }

    @Data
    public static class RouteRequest {
        private Integer topN;
        private Double quantity;           
        private String system;             
        private Boolean allowCrossSystem;  
    }
}
