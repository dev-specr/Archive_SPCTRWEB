package com.spectre.common;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Component
public class RateLimitFilter implements Filter {

    @Value("${app.ratelimit.enabled:true}")
    private boolean enabled;


    @Value("${app.ratelimit.use-forwarded:false}")
    private boolean useForwarded;

    @Value("${app.ratelimit.forwarded-header:X-Forwarded-For}")
    private String forwardedHeader;

    @Value("${app.ratelimit.realip-header:X-Real-IP}")
    private String realIpHeader;

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (!enabled || !(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        String ip = resolveClientIp(req);
        String group = resolveGroup(req.getRequestURI());
        String key = ip + ":" + group;

        Bucket bucket = buckets.computeIfAbsent(key, k -> newBucketFor(group));

        if (bucket.tryConsume(1)) {
            resp.setHeader("X-Rate-Limit-Remaining", String.valueOf(bucket.getAvailableTokens()));
            chain.doFilter(request, response);
        } else {
            resp.setStatus(429);
            resp.setContentType("application/json");
            resp.getWriter().write("{\"error\":\"Too Many Requests\"}");
        }
    }

    private String resolveGroup(String uri) {
        if (uri.startsWith("/api/auth/"))   return "auth";
        if (uri.startsWith("/api/public/")) return "public";
        return "default";
    }

    private Bucket newBucketFor(String group) {
        Bandwidth limit = "auth".equals(group)
                ? Bandwidth.builder().capacity(10).refillGreedy(10, Duration.ofMinutes(1)).build()
                : Bandwidth.builder().capacity(300).refillGreedy(300, Duration.ofMinutes(1)).build();

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    private static final Pattern COMMA_SPLIT = Pattern.compile(",");

    private String resolveClientIp(HttpServletRequest req) {
        if (useForwarded) {
            String xff = req.getHeader(forwardedHeader);
            if (xff != null && !xff.isBlank()) {
                
                String first = COMMA_SPLIT.split(xff)[0];
                if (first != null) {
                    String ip = first.trim();
                    if (!ip.isBlank()) return ip;
                }
            }
            String rip = req.getHeader(realIpHeader);
            if (rip != null && !rip.isBlank()) {
                return rip.trim();
            }
        }
        return req.getRemoteAddr();
    }
}
