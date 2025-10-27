package com.spectre.publicapi;

import com.spectre.commodity.UexCommodityService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/public/commodities")
@RequiredArgsConstructor
public class CommodityCatalogController {
    private final UexCommodityService svc;

    @GetMapping("/names")
    public List<Map<String,Object>> names(@RequestParam(name = "q", required = false) String q) {
        return svc.listAll(q).stream()
                .map(c -> {
                    Map<String,Object> m = new java.util.HashMap<>();
                    m.put("id", c.getUexId());
                    m.put("name", c.getName());
                    m.put("code", c.getCode());
                    m.put("kind", c.getKind());
                    return m;
                })
                .toList();
    }
}
