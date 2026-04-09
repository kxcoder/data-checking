package com.example.datachecking.infrastructure.cache;

import com.example.datachecking.domain.service.CacheService;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class FeatureLocalCache implements CacheService {

    private final Cache<String, CacheEntry> cache = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();

    @Override
    public void put(String key, Object value, int ttlSeconds) {
        cache.put(key, new CacheEntry(value, System.currentTimeMillis()));
    }

    @Override
    public Object get(String key) {
        CacheEntry entry = cache.getIfPresent(key);
        if (entry == null) {
            return null;
        }
        return entry.value;
    }

    @Override
    public void invalidate(String key) {
        cache.invalidate(key);
    }

    private static class CacheEntry {
        final Object value;
        final long timestamp;

        CacheEntry(Object value, long timestamp) {
            this.value = value;
            this.timestamp = timestamp;
        }
    }
}
