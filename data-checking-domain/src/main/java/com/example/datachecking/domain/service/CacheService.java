package com.example.datachecking.domain.service;

public interface CacheService {

    void put(String key, Object value, int ttlSeconds);

    Object get(String key);

    void invalidate(String key);
}
