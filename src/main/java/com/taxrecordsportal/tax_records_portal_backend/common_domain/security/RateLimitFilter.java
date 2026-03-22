package com.taxrecordsportal.tax_records_portal_backend.common_domain.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

// Rate limits auth endpoints per IP to prevent brute-force attacks.
// Only applies to endpoints listed in LIMITS — all others skip this filter.
// Buckets are cached with Caffeine for automatic eviction of inactive IPs.
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    // endpoint -> rate limit (login: 5/min, others: 3-5 per 15min)
    private static final Map<String, Bandwidth> LIMITS = Map.of(
            "/api/v1/auth/login", Bandwidth.builder().capacity(5).refillGreedy(5, Duration.ofMinutes(1)).build(),
            "/api/v1/auth/forgot-password", Bandwidth.builder().capacity(3).refillGreedy(3, Duration.ofMinutes(15)).build(),
            "/api/v1/auth/reset-password", Bandwidth.builder().capacity(5).refillGreedy(5, Duration.ofMinutes(15)).build(),
            "/api/v1/auth/set-password", Bandwidth.builder().capacity(5).refillGreedy(5, Duration.ofMinutes(15)).build()
    );

    // keyed by "IP:path", auto-evicts after 30min of inactivity
    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterAccess(Duration.ofMinutes(30))
            .build();

    // only filter auth endpoints that need rate limiting
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !LIMITS.containsKey(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String key = request.getRemoteAddr() + ":" + request.getRequestURI();
        Bucket bucket = buckets.get(key, k -> Bucket.builder()
                .addLimit(LIMITS.get(request.getRequestURI()))
                .build());

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\":\"Too many requests. Try again later.\"}");
        }
    }
}
