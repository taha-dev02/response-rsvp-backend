package com.rsvp.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, BucketWrapper> cache = new ConcurrentHashMap<>();
    private static final long CACHE_CLEANUP_INTERVAL = TimeUnit.HOURS.toMillis(1);
    private long lastCleanup = System.currentTimeMillis();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Cleanup expired entries periodically
        cleanupExpiredEntries();

        String key = getClientKey(request);
        BucketWrapper wrapper = resolveBucket(key, request);

        ConsumptionProbe probe = wrapper.bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            // Add rate limit headers for transparency
            response.setHeader("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));
            response.setHeader("X-RateLimit-Limit", String.valueOf(wrapper.limit));
            filterChain.doFilter(request, response);
        } else {
            long waitForRefill = TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill());

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", String.valueOf(waitForRefill));
            response.setHeader("X-RateLimit-Limit", String.valueOf(wrapper.limit));
            response.setHeader("X-RateLimit-Remaining", "0");
            response.setContentType("application/json");
            response.getWriter().write(String.format(
                    "{\"error\":\"Too many requests. Please try again in %d seconds.\"}",
                    waitForRefill
            ));
        }
    }

    private BucketWrapper resolveBucket(String key, HttpServletRequest request) {
        // Key includes endpoint type to separate limits
        String fullKey = key + ":" + getEndpointType(request);

        return cache.computeIfAbsent(fullKey, k -> {
            BucketWrapper wrapper = createBucketWrapper(request);
            wrapper.lastAccess = System.currentTimeMillis();
            return wrapper;
        });
    }

    private BucketWrapper createBucketWrapper(HttpServletRequest request) {
        String path = request.getRequestURI();
        BucketWrapper wrapper = new BucketWrapper();

        // Different limits for different endpoint types
        Bandwidth limit;

        if (path.contains("/sync")) {
            wrapper.limit = 100;
            limit = Bandwidth.classic(100, Refill.intervally(100, Duration.ofHours(1)));
        } else if (path.contains("/import")) {
            wrapper.limit = 10;
            limit = Bandwidth.classic(10, Refill.intervally(10, Duration.ofHours(1)));
        } else if (path.contains("/auth")) {
            wrapper.limit = 5;
            limit = Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(1)));
        } else {
            wrapper.limit = 100;
            limit = Bandwidth.classic(100, Refill.intervally(100, Duration.ofMinutes(1)));
        }

        wrapper.bucket = Bucket.builder().addLimit(limit).build();
        return wrapper;
    }

    private String getEndpointType(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path.contains("/sync")) return "sync";
        if (path.contains("/import")) return "import";
        if (path.contains("/auth")) return "auth";
        return "default";
    }

    private String getClientKey(HttpServletRequest request) {
        // For proxied requests (Render, Vercel), use X-Forwarded-For
        // But validate it's from trusted proxy
        String xForwardedFor = request.getHeader("X-Forwarded-For");

        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // Take first IP (original client)
            String clientIp = xForwardedFor.split(",")[0].trim();

            // Basic validation: ensure it's a valid IP format
            if (isValidIpAddress(clientIp)) {
                return clientIp;
            }
        }

        return request.getRemoteAddr();
    }

    private boolean isValidIpAddress(String ip) {
        // Basic IP validation (IPv4 and IPv6)
        String ipv4Pattern = "^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}$";
        String ipv6Pattern = "^(([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,7}:|([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}){1,2}|([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}){1,3}|([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}){1,4}|([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}){1,5}|[0-9a-fA-F]{1,4}:((:[0-9a-fA-F]{1,4}){1,6})|:((:[0-9a-fA-F]{1,4}){1,7}|:))$";

        return ip.matches(ipv4Pattern) || ip.matches(ipv6Pattern);
    }

    private void cleanupExpiredEntries() {
        long now = System.currentTimeMillis();

        // Run cleanup every hour
        if (now - lastCleanup > CACHE_CLEANUP_INTERVAL) {
            cache.entrySet().removeIf(entry -> {
                // Remove entries not accessed in last 2 hours
                return now - entry.getValue().lastAccess > TimeUnit.HOURS.toMillis(2);
            });
            lastCleanup = now;
        }
    }

    // Wrapper class to track bucket metadata
    private static class BucketWrapper {
        Bucket bucket;
        long lastAccess;
        int limit;
    }
}
