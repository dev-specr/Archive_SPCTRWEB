package com.spectre.commodity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UexCommodityService {
    private final UexCommodityRepository repo;
    private final RestTemplate rest;
    private final ObjectMapper om = new ObjectMapper();

    @Value("${app.uex.base-url:https:}")
    private String baseUrl;
    
    @Value("${app.uex.api-key:}")
    private String apiKey;

    @Transactional
    public int refreshCatalog() throws Exception {
        int page = 1;
        int upserts = 0;
        while (true) {
            String url = baseUrl + "/2.0/commodities/" + page;
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            if (apiKey != null && !apiKey.isBlank()) {
                headers.set("Authorization", "Bearer " + apiKey);
                headers.set("X-API-Key", apiKey);
            }
            HttpEntity<Void> req = new HttpEntity<>(headers);
            String body;
            try {
                var resp = rest.exchange(url, HttpMethod.GET, req, String.class);
                if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) break;
                body = resp.getBody();
            } catch (HttpStatusCodeException ex) {
                if (ex.getStatusCode().value() == 429) {
                    log.warn("UEX catalog rate-limited on page {}. Stop.", page);
                    break;
                }
                throw ex;
            }
            JsonNode root = om.readTree(body);
            JsonNode data = root.get("data");
            if (data == null || !data.isArray() || data.isEmpty()) break;

            for (JsonNode n : data) {
                long id = n.path("id").asLong();
                String name = text(n, "name");
                if (name == null || name.isBlank()) continue;
                UexCommodity c = repo.findById(id).orElseGet(() -> {
                    UexCommodity x = new UexCommodity();
                    x.setUexId(id);
                    return x;
                });
                c.setName(name);
                c.setCode(text(n, "code"));
                c.setKind(text(n, "kind"));
                c.setWeightScu(num(n, "weight_scu"));
                c.setBuyable(bool(n, "is_buyable"));
                c.setSellable(bool(n, "is_sellable"));
                c.setExtractable(bool(n, "is_extractable"));
                c.setMineral(bool(n, "is_mineral"));
                c.setRaw(bool(n, "is_raw"));
                c.setRefined(bool(n, "is_refined"));
                c.setWikiUrl(text(n, "wiki"));
                c.setDateAdded(epoch(n, "date_added"));
                c.setDateModified(epoch(n, "date_modified"));
                repo.save(c);
                upserts++;
            }

            if (data.size() < 100) break; 
            page++;
            if (page > 2000) break; 
            try { Thread.sleep(100); } catch (InterruptedException ignored) {}
        }
        return upserts;
    }

    public List<UexCommodity> listAll(String q) {
        if (q == null || q.isBlank()) return repo.findAllByOrderByNameAsc();
        return repo.findAllByNameContainingIgnoreCaseOrderByNameAsc(q.trim());
    }

    private String text(JsonNode n, String k) { JsonNode v = n.get(k); return (v==null||v.isNull())?null:v.asText(); }
    private Double num(JsonNode n, String k) { JsonNode v = n.get(k); return (v==null||v.isNull())?null:v.asDouble(); }
    private Boolean bool(JsonNode n, String k) { JsonNode v = n.get(k); return (v==null||v.isNull())?null:(v.asInt(0)==1); }
    private Instant epoch(JsonNode n, String k) { JsonNode v = n.get(k); return (v==null||v.isNull())?null:Instant.ofEpochSecond(v.asLong()); }
}

