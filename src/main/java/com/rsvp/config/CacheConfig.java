package com.rsvp.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;


@Configuration
@EnableCaching
public class CacheConfig {


    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                "events",              // Cache for event lists
                "eventById",           // Cache for single event lookups
                "guests",              // Cache for guest lists
                "guestById",           // Cache for single guest lookups
                "rsvps",               // Cache for RSVP lists
                "rsvpMetrics",         // Cache for RSVP counts/stats
                "dashboardMetrics",    // Cache for dashboard data
                "groupNames"           // Cache for group name lists
        );

        cacheManager.setCaffeine(caffeineCacheBuilder());
        return cacheManager;
    }


    private Caffeine<Object, Object> caffeineCacheBuilder() {
        return Caffeine.newBuilder()
                // Maximum number of entries in cache
                .maximumSize(1000)

                // Time-to-live: Cache entries expire after 5 minutes
                .expireAfterWrite(5, TimeUnit.MINUTES)

                // Time-to-idle: Entries expire if not accessed for 10 minutes
                .expireAfterAccess(10, TimeUnit.MINUTES)

                // Enable statistics for monitoring
                .recordStats()

                // Initial capacity to reduce resizing
                .initialCapacity(100);
    }

    /**
     * 🔧 OPTIONAL: Separate cache configurations
     * Uncomment and customize if you need different TTLs per cache
     */

    // @Bean
    // public CacheManager customCacheManager() {
    //     SimpleCacheManager cacheManager = new SimpleCacheManager();
    //
    //     List<CaffeineCache> caches = List.of(
    //         // Events: 5 minute TTL (events don't change often)
    //         buildCache("events", 5, TimeUnit.MINUTES, 500),
    //         buildCache("eventById", 5, TimeUnit.MINUTES, 500),
    //
    //         // Guests: 10 minute TTL (guest lists are stable)
    //         buildCache("guests", 10, TimeUnit.MINUTES, 1000),
    //         buildCache("guestById", 10, TimeUnit.MINUTES, 1000),
    //
    //         // RSVPs: 2 minute TTL (RSVPs update frequently)
    //         buildCache("rsvps", 2, TimeUnit.MINUTES, 1000),
    //         buildCache("rsvpMetrics", 2, TimeUnit.MINUTES, 200),
    //
    //         // Dashboard: 1 minute TTL (real-time data)
    //         buildCache("dashboardMetrics", 1, TimeUnit.MINUTES, 100),
    //
    //         // Group names: 15 minute TTL (rarely changes)
    //         buildCache("groupNames", 15, TimeUnit.MINUTES, 100)
    //     );
    //
    //     cacheManager.setCaches(caches);
    //     return cacheManager;
    // }

    // private CaffeineCache buildCache(String name, int ttl, TimeUnit unit, int maxSize) {
    //     return new CaffeineCache(name, Caffeine.newBuilder()
    //         .expireAfterWrite(ttl, unit)
    //         .maximumSize(maxSize)
    //         .recordStats()
    //         .build());
    // }
}