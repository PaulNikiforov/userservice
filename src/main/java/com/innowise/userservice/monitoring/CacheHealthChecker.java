package com.innowise.userservice.monitoring;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Component for monitoring and managing cache health.
 *
 * <p>Provides methods to inspect cache state and clear caches manually.
 */
@Slf4j
@Component
public class CacheHealthChecker {

    private final CacheManager cacheManager;

    public CacheHealthChecker(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * Returns the names of all registered caches.
     *
     * @return map of cache names to their availability status
     */
    public Map<String, Boolean> getCacheStatus() {
        Map<String, Boolean> status = new HashMap<>();
        cacheManager.getCacheNames().forEach(cacheName -> {
            var cache = cacheManager.getCache(cacheName);
            status.put(cacheName, cache != null);
        });
        return status;
    }

    /**
     * Clears all caches.
     */
    public void clearAllCaches() {
        log.info("Clearing all caches");
        cacheManager.getCacheNames().forEach(cacheName -> {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
                log.debug("Cleared cache: {}", cacheName);
            }
        });
    }

    /**
     * Clears a specific cache by name.
     *
     * @param cacheName the name of the cache to clear
     * @return true if the cache was found and cleared, false otherwise
     */
    public boolean clearCache(String cacheName) {
        var cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            log.info("Clearing cache: {}", cacheName);
            cache.clear();
            return true;
        }
        log.warn("Cache not found: {}", cacheName);
        return false;
    }
}
