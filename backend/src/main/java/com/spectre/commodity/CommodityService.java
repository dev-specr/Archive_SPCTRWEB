package com.spectre.commodity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommodityService {

    private final CommodityPriceRepository repo;
    private final ObjectMapper om = new ObjectMapper();
    private final RestTemplate rest;
    private final UexCommodityRepository commodityCatalog;
    private final java.util.concurrent.locks.ReentrantLock refreshLock = new java.util.concurrent.locks.ReentrantLock();

    @Value("${app.uex.base-url:https:}")
    private String baseUrl;
    @Value("${app.uex.endpoint:/2.0/items_prices_all}")
    private String endpoint;
    @Value("${app.uex.api-key:}")
    private String apiKey;
    
    @Value("${app.uex.allowed-category-ids:}")
    private String allowedCategoryIdsCsv;
    @Value("${app.uex.excluded-category-ids:32}")
    private String excludedCategoryIdsCsv;
    
    @Value("${app.uex.allowed-kinds:Metal,Mineral,Agricultural,Food,Chemical,Processed}")
    private String allowedKindsCsv;

    
    @Scheduled(cron = "${app.uex.refresh-cron:0 0 * * * *}")
    public void hourlyRefresh() {
        try {
            int n = refreshFromUex();
            log.info("UEX refresh complete. Upserted {} records.", n);
        } catch (Exception e) {
            log.error("UEX refresh failed: {}", e.getMessage(), e);
        }
    }

    @Transactional
    public int refreshFromUex() throws Exception {
        if (!refreshLock.tryLock()) {
            log.warn("UEX refresh already in progress; skipping concurrent request.");
            return 0;
        }
        try {
            String url = baseUrl + endpoint;
            var headers = new org.springframework.http.HttpHeaders();
            headers.setAccept(List.of(org.springframework.http.MediaType.APPLICATION_JSON));
            if (apiKey != null && !apiKey.isBlank()) {
                headers.set("Authorization", "Bearer " + apiKey);
                headers.set("X-API-Key", apiKey);
            }
            var req = new org.springframework.http.HttpEntity<Void>(headers);
            String body;
            try {
                var resp = rest.exchange(url, org.springframework.http.HttpMethod.GET, req, String.class);
                log.info("UEX fetch status={} length={}", resp.getStatusCode().value(), resp.getBody() == null ? 0 : resp.getBody().length());
                body = resp.getBody();
            } catch (HttpStatusCodeException ex) {
                if (ex.getStatusCode().value() == 429) {
                    log.warn("UEX rate limited on full feed; skipping this run. bodyLen={}", ex.getResponseBodyAsString() == null ? 0 : ex.getResponseBodyAsString().length());
                    return 0;
                } else {
                    log.warn("UEX full feed failed status={} bodyLen={}", ex.getStatusCode().value(), ex.getResponseBodyAsString() == null ? 0 : ex.getResponseBodyAsString().length());
                    throw ex;
                }
            }
            if (body == null || body.isBlank()) return 0;

            JsonNode root = om.readTree(body);
            JsonNode items = root.has("data") ? root.get("data") : root; 
            int size = (items != null && items.isArray()) ? items.size() : -1;
            log.info("UEX items array size={}", size);
            if (items == null || !items.isArray()) return 0;

            int upserts = 0;
            for (JsonNode n : items) {
                UpsertRow row = toRow(n);
                if (row == null || row.commodity == null || row.location == null) continue;
            if (!categoryAllowed(row.categoryId)) continue;
            if (!kindAllowedByCatalog(row.commodityId)) continue;
            CommodityPrice e = null;
                if (row.commodityId != null && row.locationId != null) {
                    e = repo.findByUexCommodityIdAndUexLocationId(row.commodityId, row.locationId).orElse(null);
                }
            if (e == null) {
                e = repo.findByCommodityIgnoreCaseAndLocationIgnoreCase(row.commodity, row.location).orElse(null);
            }
            boolean existed = (e != null);
            if (e == null) e = new CommodityPrice();
            Double oldBuy = existed ? e.getBuy() : null;
            Double oldSell = existed ? e.getSell() : null;
            Instant oldUpdated = existed ? e.getUpdatedAt() : null;
            e.setCommodity(row.commodity);
            e.setLocation(row.location);
            e.setSystem(row.system);
            
            if (existed) {
                e.setPrevBuy(oldBuy);
                e.setPrevSell(oldSell);
                e.setPrevUpdatedAt(oldUpdated);
            }
            e.setBuy(row.buy);
            e.setSell(row.sell);
            e.setCurrency("aUEC");
            e.setUexCommodityId(row.commodityId);
            e.setUexLocationId(row.locationId);
            e.setUexCategoryId(row.categoryId);
            e.setUpdatedAt(Instant.now());
            try {
                repo.save(e);
            } catch (org.springframework.dao.DataIntegrityViolationException dup) {
                CommodityPrice existing = repo.findByCommodityIgnoreCaseAndLocationIgnoreCase(row.commodity, row.location).orElse(null);
                if (existing != null) {
                    existing.setSystem(row.system);
                    existing.setPrevBuy(existing.getBuy());
                    existing.setPrevSell(existing.getSell());
                    existing.setPrevUpdatedAt(existing.getUpdatedAt());
                    existing.setBuy(row.buy);
                    existing.setSell(row.sell);
                    existing.setCurrency("aUEC");
                    existing.setUexCommodityId(row.commodityId);
                    existing.setUexLocationId(row.locationId);
                    existing.setUpdatedAt(Instant.now());
                    repo.save(existing);
                } else {
                    throw dup;
                }
            }
            upserts++;
        }
        return upserts;
        } finally {
            try { refreshLock.unlock(); } catch (IllegalMonitorStateException ignore) {}
        }
    }

    private int refreshViaCommodityIds() throws Exception {
        List<CommodityMeta> commodities = fetchAllCommodities();
        log.info("UEX commodities meta count={}", commodities.size());
        int upserts = 0;
        for (CommodityMeta cm : commodities) {
            List<JsonNode> prices = fetchPricesForCommodity(cm.id());
            for (JsonNode p : prices) {
                Long cid = nl(p, "id_commodity");
                Long tid = nl(p, "id_terminal");
                if (cid == null || tid == null) continue;
                String system = nt(p, "star_system_name");
                String terminal = nt(p, "terminal_name");
                if (terminal == null) terminal = nt(p, "space_station_name");
                if (terminal == null) terminal = nt(p, "city_name");
                if (terminal == null) terminal = nt(p, "planet_name");
                Double buy = nd(p, "price_buy");
                Double sell = nd(p, "price_sell");
                String commodity = cm.name();

                if (commodity == null || terminal == null) continue;

                CommodityPrice e = repo.findByUexCommodityIdAndUexLocationId(cid, tid).orElse(null);
                if (e == null) e = repo.findByCommodityIgnoreCaseAndLocationIgnoreCase(commodity, terminal).orElse(null);
                if (e == null) e = new CommodityPrice();

                e.setCommodity(commodity);
                e.setLocation(terminal);
                e.setSystem(system);
                e.setBuy(buy);
                e.setSell(sell);
                e.setCurrency("aUEC");
                e.setUexCommodityId(cid);
                e.setUexLocationId(tid);
                e.setUpdatedAt(Instant.now());

                try {
                    repo.save(e);
                } catch (org.springframework.dao.DataIntegrityViolationException dup) {
                    CommodityPrice existing = repo.findByCommodityIgnoreCaseAndLocationIgnoreCase(commodity, terminal).orElse(null);
                    if (existing != null) {
                        existing.setSystem(system);
                        existing.setBuy(buy);
                        existing.setSell(sell);
                        existing.setCurrency("aUEC");
                        existing.setUexCommodityId(cid);
                        existing.setUexLocationId(tid);
                        existing.setUpdatedAt(Instant.now());
                        repo.save(existing);
                    } else {
                        throw dup;
                    }
                }
                upserts++;
            }
            try { Thread.sleep(100); } catch (InterruptedException ignored) {}
        }
        return upserts;
    }

    private List<CommodityMeta> fetchAllCommodities() throws Exception {
        List<CommodityMeta> out = new ArrayList<>();
        int page = 1;
        while (true) {
            String url = baseUrl + "/2.0/commodities/" + page;
            var headers = new org.springframework.http.HttpHeaders();
            headers.setAccept(List.of(org.springframework.http.MediaType.APPLICATION_JSON));
            if (apiKey != null && !apiKey.isBlank()) {
                headers.set("Authorization", "Bearer " + apiKey);
                headers.set("X-API-Key", apiKey);
            }
            var req = new org.springframework.http.HttpEntity<Void>(headers);
            var resp = rest.exchange(url, org.springframework.http.HttpMethod.GET, req, String.class);
            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) break;
            JsonNode root = om.readTree(resp.getBody());
            JsonNode data = root.get("data");
            if (data == null || !data.isArray() || data.isEmpty()) break;
            for (JsonNode n : data) {
                long id = n.path("id").asLong();
                String name = nt(n, "name");
                boolean buyable = n.path("is_buyable").asInt(0) == 1;
                boolean sellable = n.path("is_sellable").asInt(0) == 1;
                if (!buyable && !sellable) continue; 
                out.add(new CommodityMeta(id, name));
            }
            if (data.size() < 100) break; 
            page++;
            if (page > 1000) break; 
            try { Thread.sleep(100); } catch (InterruptedException ignored) {}
        }
        return out;
    }

    private List<JsonNode> fetchPricesForCommodity(long commodityId) throws Exception {
        String url = baseUrl + "/2.0/commodities_prices?id_commodity=" + commodityId;
        var headers = new org.springframework.http.HttpHeaders();
        headers.setAccept(List.of(org.springframework.http.MediaType.APPLICATION_JSON));
        if (apiKey != null && !apiKey.isBlank()) {
            headers.set("Authorization", "Bearer " + apiKey);
            headers.set("X-API-Key", apiKey);
        }
        var req = new org.springframework.http.HttpEntity<Void>(headers);
        var resp = rest.exchange(url, org.springframework.http.HttpMethod.GET, req, String.class);
        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) return List.of();
        JsonNode root = om.readTree(resp.getBody());
        JsonNode data = root.get("data");
        if (data == null || !data.isArray()) return List.of();
        List<JsonNode> out = new ArrayList<>();
        data.forEach(out::add);
        return out;
    }

    private String nt(JsonNode n, String key) { JsonNode v = n.get(key); return (v==null||v.isNull())?null:v.asText(); }
    private Long nl(JsonNode n, String key) { JsonNode v = n.get(key); return (v==null||v.isNull())?null:v.asLong(); }
    private Double nd(JsonNode n, String key) { JsonNode v = n.get(key); return (v==null||v.isNull())?null:v.asDouble(); }

    record CommodityMeta(long id, String name) {}

    
    public Map<String, Object> diagnostics() throws Exception {
        String url = baseUrl + endpoint;
        var headers = new org.springframework.http.HttpHeaders();
        headers.setAccept(List.of(org.springframework.http.MediaType.APPLICATION_JSON));
        if (apiKey != null && !apiKey.isBlank()) {
            headers.set("Authorization", "Bearer " + apiKey);
            headers.set("X-API-Key", apiKey);
        }
        var req = new org.springframework.http.HttpEntity<Void>(headers);
        var resp = rest.exchange(url, org.springframework.http.HttpMethod.GET, req, String.class);
        String body = resp.getBody();
        int status = resp.getStatusCode().value();
        int len = body == null ? 0 : body.length();
        Map<String,Object> out = new java.util.HashMap<>();
        out.put("url", url);
        out.put("status", status);
        out.put("length", len);
        if (body != null && len > 0) {
            JsonNode root = om.readTree(body);
            JsonNode items = root.has("data") ? root.get("data") : root;
            int size = (items != null && items.isArray()) ? items.size() : -1;
            out.put("itemsSize", size);
            java.util.List<Map<String,Object>> sample = new java.util.ArrayList<>();
            if (items != null && items.isArray()) {
                int count = 0;
                for (JsonNode n : items) {
                    UpsertRow r = toRow(n);
                    if (r != null) {
                        sample.add(Map.of(
                                "commodity", r.commodity,
                                "location", r.location,
                                "system", r.system,
                                "buy", r.buy,
                                "sell", r.sell
                        ));
                        count++;
                        if (count >= 3) break;
                    }
                }
            }
            out.put("sample", sample);
        }
        return out;
    }

    
    private UpsertRow toRow(JsonNode n) {
        String commodity = firstText(n, "commodity", "name", "item", "good", "item_name");
        String location = firstText(n, "location", "station", "place", "market", "city", "terminal_name");
        if (location == null && n.has("location") && n.get("location").has("name"))
            location = n.get("location").get("name").asText();
        String system = firstText(n, "system", "star_system");
        Double buy = firstDouble(n, "buy", "buy_price", "buyPrice", "b", "price_buy");
        Double sell = firstDouble(n, "sell", "sell_price", "sellPrice", "s", "price_sell");
        Long commodityId = firstLong(n, "commodity_id", "item_id", "good_id", "cid", "id_item");
        Long locationId = firstLong(n, "location_id", "lid", "id_terminal");
        Long categoryId = firstLong(n, "id_category", "category_id");

        if (commodity == null || location == null) return null;
        UpsertRow r = new UpsertRow();
        r.commodity = commodity.trim();
        r.location = location.trim();
        r.system = system;
        r.buy = buy;
        r.sell = sell;
        r.commodityId = commodityId;
        r.locationId = locationId;
        r.categoryId = categoryId;
        return r;
    }

    private String firstText(JsonNode n, String... keys) {
        for (String k : keys) {
            if (n.has(k) && !n.get(k).isNull()) return n.get(k).asText();
        }
        return null;
    }
    private Long firstLong(JsonNode n, String... keys) {
        for (String k : keys) {
            if (n.has(k) && !n.get(k).isNull()) return n.get(k).asLong();
        }
        return null;
    }
    private Double firstDouble(JsonNode n, String... keys) {
        for (String k : keys) {
            if (n.has(k) && !n.get(k).isNull()) return n.get(k).asDouble();
        }
        return null;
    }

    
    public List<CommoditySummary> summarize(String q) {
        List<String> commodities = (q == null || q.isBlank())
                ? repo.listCommodities()
                : repo.listCommodities().stream()
                    .filter(name -> name.toLowerCase().contains(q.toLowerCase()))
                    .collect(Collectors.toList());

        List<CommoditySummary> out = new ArrayList<>();
        for (String c : commodities) {
            List<CommodityPrice> rows = repo.findAllByCommodityIgnoreCase(c);
            if (rows.isEmpty()) continue;
            CommodityPrice bestBuy = null, bestSell = null;
            for (CommodityPrice r : rows) {
                if (r.getBuy() != null) {
                    if (bestBuy == null || r.getBuy() < bestBuy.getBuy()) bestBuy = r;
                }
                if (r.getSell() != null) {
                    if (bestSell == null || r.getSell() > bestSell.getSell()) bestSell = r;
                }
            }
            if (bestBuy == null || bestSell == null) continue;
            double spread = bestSell.getSell() - bestBuy.getBuy();
            if (spread <= 0) continue;
            Double buyDelta = (bestBuy.getPrevBuy() == null) ? null : bestBuy.getBuy() - bestBuy.getPrevBuy();
            Double sellDelta = (bestSell.getPrevSell() == null) ? null : bestSell.getSell() - bestSell.getPrevSell();
            out.add(new CommoditySummary(c, bestBuy.getLocation(), bestBuy.getBuy(), buyDelta,
                    bestSell.getLocation(), bestSell.getSell(), sellDelta, spread));
        }
        
        out.sort((a,b) -> Double.compare(b.spread(), a.spread()));
        return out;
    }

    public List<RouteProposal> bestRoutes(Integer topN, Double quantity, String system, boolean allowCrossSystem) {
        var summaries = summarize(null);
        List<RouteProposal> routes = new ArrayList<>();
        for (var s : summaries) {
            boolean systemOk = true;
            if (system != null && !system.isBlank() && !allowCrossSystem) {
                systemOk = Objects.equals(systemOf(s.buyLocation()), system) && Objects.equals(systemOf(s.sellLocation()), system);
            }
            if (!systemOk) continue;
            double qty = (quantity == null || quantity <= 0) ? 1.0 : quantity;
            double profit = s.spread() * qty;
            routes.add(new RouteProposal(s.commodity(), s.buyLocation(), s.buyPrice(), s.sellLocation(), s.sellPrice(), s.spread(), qty, profit));
        }
        routes.sort((a,b) -> Double.compare(b.profit(), a.profit()));
        if (topN == null || topN <= 0) topN = 10;
        return routes.subList(0, Math.min(topN, routes.size()));
    }

    private String systemOf(String location) {
        if (location == null) return null;
        
        
        return null;
    }

    
    public record CommoditySummary(String commodity,
                                   String buyLocation, Double buyPrice, Double buyChange,
                                   String sellLocation, Double sellPrice, Double sellChange,
                                   Double spread) {}
    public record RouteProposal(String commodity, String buyLocation, Double buyPrice, String sellLocation, Double sellPrice, Double spread, Double quantity, Double profit) {}

    static class UpsertRow {
        Long commodityId;
        Long locationId;
        Long categoryId;
        String commodity;
        String location;
        String system;
        Double buy;
        Double sell;
    }

    private boolean categoryAllowed(Long categoryId) {
        if (categoryId == null) return true; 
        java.util.Set<Long> allow = parseIdSet(allowedCategoryIdsCsv);
        if (!allow.isEmpty()) return allow.contains(categoryId);
        java.util.Set<Long> deny = parseIdSet(excludedCategoryIdsCsv);
        return !deny.contains(categoryId);
    }

    private java.util.Set<Long> parseIdSet(String csv) {
        java.util.Set<Long> out = new java.util.HashSet<>();
        if (csv == null || csv.isBlank()) return out;
        for (String p : csv.split(",")) {
            try { out.add(Long.parseLong(p.trim())); } catch (NumberFormatException ignore) {}
        }
        return out;
    }

    private boolean kindAllowedByCatalog(Long commodityId) {
        if (commodityId == null) return true;
        try {
            var opt = commodityCatalog.findById(commodityId);
            if (opt.isEmpty()) return true; 
            String kind = opt.get().getKind();
            if (kind == null || kind.isBlank()) return true;
            java.util.Set<String> allowed = new java.util.HashSet<>();
            for (String k : allowedKindsCsv.split(",")) allowed.add(k.trim().toLowerCase());
            return allowed.contains(kind.trim().toLowerCase());
        } catch (Exception ignore) {
            return true;
        }
    }

    
    public Map<String, Object> testCommodity(long commodityId) throws Exception {
        List<JsonNode> rows = fetchPricesForCommodity(commodityId);
        Map<String, Object> out = new HashMap<>();
        out.put("id", commodityId);
        out.put("entries", rows.size());
        String commodityName = rows.isEmpty() ? null : nt(rows.get(0), "commodity_name");
        out.put("commodity", commodityName);

        JsonNode bestBuy = null, bestSell = null;
        for (JsonNode n : rows) {
            Double buy = nd(n, "price_buy");
            Double sell = nd(n, "price_sell");
            if (buy != null && buy > 0) {
                if (bestBuy == null || buy < nd(bestBuy, "price_buy")) bestBuy = n;
            }
            if (sell != null && sell > 0) {
                if (bestSell == null || sell > nd(bestSell, "price_sell")) bestSell = n;
            }
        }
        if (bestBuy != null) {
            out.put("bestBuy", Map.of(
                    "location", firstNonNull(nt(bestBuy, "terminal_name"), nt(bestBuy, "space_station_name"), nt(bestBuy, "city_name"), nt(bestBuy, "planet_name")),
                    "price", nd(bestBuy, "price_buy"),
                    "system", nt(bestBuy, "star_system_name")
            ));
        }
        if (bestSell != null) {
            out.put("bestSell", Map.of(
                    "location", firstNonNull(nt(bestSell, "terminal_name"), nt(bestSell, "space_station_name"), nt(bestSell, "city_name"), nt(bestSell, "planet_name")),
                    "price", nd(bestSell, "price_sell"),
                    "system", nt(bestSell, "star_system_name")
            ));
        }
        List<Map<String,Object>> sample = new ArrayList<>();
        for (int i = 0; i < Math.min(5, rows.size()); i++) {
            JsonNode n = rows.get(i);
            sample.add(Map.of(
                    "location", firstNonNull(nt(n, "terminal_name"), nt(n, "space_station_name"), nt(n, "city_name"), nt(n, "planet_name")),
                    "buy", nd(n, "price_buy"),
                    "sell", nd(n, "price_sell"),
                    "system", nt(n, "star_system_name")
            ));
        }
        out.put("sample", sample);
        return out;
    }

    private String firstNonNull(String... vals) {
        for (String v : vals) if (v != null && !v.isBlank()) return v;
        return null;
    }

    
    public BestQuote bestFromDb(Long commodityId, String name) {
        List<CommodityPrice> rows;
        if (commodityId != null) {
            rows = repo.findAllByUexCommodityId(commodityId);
        } else if (name != null && !name.isBlank()) {
            rows = repo.findAllByCommodityIgnoreCase(name);
        } else {
            rows = List.of();
        }
        if (rows.isEmpty()) return null;

        CommodityPrice bestBuy = null, bestSell = null;
        for (CommodityPrice r : rows) {
            if (r.getBuy() != null && r.getBuy() > 0) {
                if (bestBuy == null || r.getBuy() < bestBuy.getBuy()) bestBuy = r;
            }
            if (r.getSell() != null && r.getSell() > 0) {
                if (bestSell == null || r.getSell() > bestSell.getSell()) bestSell = r;
            }
        }
        String commodity = rows.get(0).getCommodity();
        Double spread = (bestBuy != null && bestSell != null) ? (bestSell.getSell() - bestBuy.getBuy()) : null;
        Endpoint buy = (bestBuy == null) ? null : new Endpoint(bestBuy.getLocation(), bestBuy.getSystem(), bestBuy.getBuy());
        Endpoint sell = (bestSell == null) ? null : new Endpoint(bestSell.getLocation(), bestSell.getSystem(), bestSell.getSell());
        return new BestQuote(commodity, buy, sell, spread);
    }

    public record Endpoint(String location, String system, Double price) {}
    public record BestQuote(String commodity, Endpoint bestBuy, Endpoint bestSell, Double spread) {}
}
