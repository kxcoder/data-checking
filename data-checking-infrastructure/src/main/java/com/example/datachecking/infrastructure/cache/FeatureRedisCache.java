package com.example.datachecking.infrastructure.cache;

import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class FeatureRedisCache {

    private final RedisTemplate<String, Object> redisTemplate;
    
    private static final String KEY_PREFIX = "feature:";

    public void put(String key, Object value, int ttlSeconds) {
        try {
            String redisKey = KEY_PREFIX + key;
            if (ttlSeconds > 0) {
                redisTemplate.opsForValue().set(redisKey, JSON.toJSONString(value), ttlSeconds, TimeUnit.SECONDS);
            } else {
                redisTemplate.opsForValue().set(redisKey, JSON.toJSONString(value));
            }
        } catch (Exception e) {
            log.error("Redis cache put failed: key={}", key, e);
        }
    }

    public Object get(String key) {
        try {
            String redisKey = KEY_PREFIX + key;
            Object value = redisTemplate.opsForValue().get(redisKey);
            if (value != null) {
                return JSON.parseObject(value.toString(), Object.class);
            }
        } catch (Exception e) {
            log.error("Redis cache get failed: key={}", key, e);
        }
        return null;
    }

    public void invalidate(String key) {
        try {
            redisTemplate.delete(KEY_PREFIX + key);
        } catch (Exception e) {
            log.error("Redis cache invalidate failed: key={}", key, e);
        }
    }
}
