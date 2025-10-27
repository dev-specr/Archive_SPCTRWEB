package com.spectre.ship;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;


@Service
@RequiredArgsConstructor
@Slf4j
public class ShipSyncService {

    private final ShipRepository repo;
    private final ObjectMapper om = new ObjectMapper();
    private final RestTemplate rest;

    @Value("${app.scwiki.base-url:https://api.star-citizen.wiki}")
    private String baseUrl;

    @Value("${app.scwiki.page-size:100}")
    private int pageSize;

    @Value("${app.scwiki.api-key:}")
    private String apiKey;

    
    @Scheduled(cron = "${app.scwiki.refresh-cron:0 10 3 * * SUN}")
    @CacheEvict(value = {"shipNames"}, allEntries = true)
    public void nightlySync() {
        try {
            int total = syncAll();
            log.info("Ship nightly sync finished. Upserted {} entries.", total);
        } catch (Exception e) {
            log.error("Ship nightly sync failed: {}", e.getMessage(), e);
        }
    }

    @Transactional
    @CacheEvict(value = {"shipNames"}, allEntries = true)
    public int syncAll() throws Exception {
        int page = 1;
        int totalUpserts = 0;
        while (true) {
            String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/api/v3/vehicles")
                    .queryParam("page[number]", page)
                    .queryParam("page[size]", pageSize)
                    .toUriString();

            HttpHeaders headers = new HttpHeaders();
            if (apiKey != null && !apiKey.isBlank()) {
                headers.set("X-API-Key", apiKey);
            }
            HttpEntity<Void> req = new HttpEntity<>(headers);
            ResponseEntity<String> resp = rest.exchange(url, HttpMethod.GET, req, String.class);
            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) break;

            JsonNode root = om.readTree(resp.getBody());
            JsonNode data = root.get("data");
            if (data == null || !data.isArray() || data.isEmpty()) break;

            for (JsonNode item : data) {
                upsertFromNode(item);
                totalUpserts++;
            }

            if (data.size() < pageSize) break; 
            page++;
        }
        return totalUpserts;
    }

    private void upsertFromNode(JsonNode item) {
        try {
            String uuid = item.path("id").asText(null);
            if (uuid == null || uuid.isBlank()) uuid = item.path("uuid").asText(null);
            JsonNode attrs = item.path("attributes");

            String name = n(attrs, "name");
            if (name == null) name = n(item, "name");
            String manufacturer = n(attrs, "manufacturer");
            String type = n(attrs, "type");
            String focus = n(attrs, "focus");
            String size = n(attrs, "size");
            Integer cargo = firstInt(attrs, item, "cargo_capacity");
            Integer scm = firstInt(attrs, item, "scm_speed");
            Integer nav = firstInt(attrs, item, "afterburner_speed");
            Double pitch = firstDouble(attrs, item, "pitch");
            Double yaw = firstDouble(attrs, item, "yaw");
            Double roll = firstDouble(attrs, item, "roll");
            Integer hp = firstInt(attrs, item, "hitpoints");
            String pledge = n(attrs, "pledge_url");
            String description = n(attrs, "description");

            
            if ((manufacturer == null || type == null || cargo == null || scm == null) && item.has("link")) {
                JsonNode details = fetchJson(item.get("link").asText());
                if (details != null && !details.isMissingNode()) {
                    JsonNode dnode = details.has("data") ? details.get("data") : details;
                    if (name == null) name = n(dnode, "name");
                    if (manufacturer == null) {
                        if (dnode.has("manufacturer") && dnode.get("manufacturer").has("name")) {
                            manufacturer = dnode.get("manufacturer").get("name").asText();
                        }
                    }
                    if (type == null) type = text(dnode.path("type"));
                    if (focus == null) focus = firstFocus(dnode.path("foci"));
                    if (size == null) size = null; 
                    if (cargo == null) cargo = i(dnode, "cargo_capacity");
                    if (scm == null) scm = i(dnode.path("speed"), "scm");
                    if (nav == null) nav = i(dnode.path("speed"), "max");
                    if (pitch == null) pitch = d(dnode.path("agility"), "pitch");
                    if (yaw == null) yaw = d(dnode.path("agility"), "yaw");
                    if (roll == null) roll = d(dnode.path("agility"), "roll");
                    if (hp == null) hp = null; 
                    if (pledge == null) pledge = null;
                    if (description == null) description = null;
                    item = dnode; 
                }
            }

            Ship s = null;
            if (uuid != null && !uuid.isBlank()) {
                s = repo.findFirstByUuid(uuid).orElse(null);
            }
            if (s == null) {
                s = (name != null && !name.isBlank()) ? repo.findFirstByNameIgnoreCase(name).orElse(null) : null;
            }
            if (s == null) s = new Ship();
            s.setUuid(uuid);

            String incomingName = (name != null && !name.isBlank()) ? name : (uuid != null ? uuid : null);
            String existingName = s.getName();
            if (existingName != null && !existingName.isBlank() && !"Unknown".equalsIgnoreCase(existingName)) {
                
                if (incomingName != null && !"Unknown".equalsIgnoreCase(incomingName) && !incomingName.equalsIgnoreCase(existingName)) {
                    s.setName(incomingName);
                }
            } else {
                s.setName(incomingName != null ? incomingName : "Unknown");
            }
            s.setManufacturer(manufacturer);
            s.setType(type);
            s.setFocus(focus);
            s.setSize(size);
            s.setCargoCapacity(cargo);
            s.setScmSpeed(scm);
            s.setNavMaxSpeed(nav);
            s.setPitch(pitch);
            s.setYaw(yaw);
            s.setRoll(roll);
            s.setHp(hp);
            s.setPledgeUrl(pledge);
            s.setDescription(description);
            s.setRawJson(item.toString());

            repo.save(s);
        } catch (Exception e) {
            log.warn("Failed to upsert ship from node: {}", e.getMessage());
        }
    }

    private JsonNode fetchJson(String url) {
        try {
            HttpHeaders headers = new HttpHeaders();
            if (apiKey != null && !apiKey.isBlank()) headers.set("X-API-Key", apiKey);
            HttpEntity<Void> req = new HttpEntity<>(headers);
            ResponseEntity<String> resp = rest.exchange(url, HttpMethod.GET, req, String.class);
            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) return null;
            return om.readTree(resp.getBody());
        } catch (Exception e) {
            log.warn("Fetch details failed for {}: {}", url, e.getMessage());
            return null;
        }
    }

    private String n(JsonNode node, String key) {
        JsonNode v = node.get(key);
        return (v == null || v.isNull()) ? null : v.asText();
    }

    private Integer i(JsonNode node, String key) {
        JsonNode v = node.get(key);
        return (v == null || v.isNull()) ? null : v.asInt();
    }

    private Double d(JsonNode node, String key) {
        JsonNode v = node.get(key);
        return (v == null || v.isNull()) ? null : v.asDouble();
    }
    private String text(JsonNode node) {
        if (node == null || node.isNull()) return null;
        if (node.isTextual()) return node.asText();
        
        if (node.has("en_EN")) return node.get("en_EN").asText();
        return null;
    }
    private String firstFocus(JsonNode foci) {
        if (foci == null || foci.isNull() || !foci.isArray() || foci.size() == 0) return null;
        JsonNode f = foci.get(0);
        if (f.isTextual()) return f.asText();
        if (f.has("en_EN")) return f.get("en_EN").asText();
        return null;
    }

    private Integer firstInt(JsonNode a, JsonNode b, String key) {
        Integer x = i(a, key);
        return x != null ? x : i(b, key);
    }
    private Double firstDouble(JsonNode a, JsonNode b, String key) {
        Double x = d(a, key);
        return x != null ? x : d(b, key);
    }
}
