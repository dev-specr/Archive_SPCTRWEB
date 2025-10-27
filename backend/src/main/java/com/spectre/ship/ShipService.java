package com.spectre.ship;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class ShipService {

    private final ShipRepository repo;

    @Cacheable("shipNames")
    public List<String> names() {
        return repo.listAllNames();
    }

    public Ship infoByName(String name) {
        return repo.findFirstByNameIgnoreCase(name)
                .orElseGet(() -> {
                    
                    List<Ship> hits = repo.searchByNameLike(name);
                    if (hits.isEmpty()) throw new ResponseStatusException(NOT_FOUND, "Ship not found");
                    return hits.get(0);
                });
    }

    public List<String> searchNames(String q, Integer limit) {
        if (q == null || q.isBlank()) return List.of();
        List<Ship> hits = repo.searchByNameLike(q);
        List<String> names = hits.stream().map(Ship::getName).toList();
        if (limit == null || limit <= 0) return names;
        return names.subList(0, Math.min(limit, names.size()));
    }
}
