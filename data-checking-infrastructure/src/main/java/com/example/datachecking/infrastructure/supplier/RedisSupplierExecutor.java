package com.example.datachecking.infrastructure.supplier;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.example.datachecking.domain.model.SupplierType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisSupplierExecutor {

    private final RedisTemplate<String, Object> redisTemplate;
    
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{(\\w+)\\}");

    public Object execute(String configJson, Map<String, Object> params) {
        JSONObject config = JSON.parseObject(configJson);
        
        String command = config.getString("command");
        String keyPattern = config.getString("key");
        String actualKey = replacePlaceholders(keyPattern, params);
        
        try {
            return switch (command.toUpperCase()) {
                case "GET" -> redisTemplate.opsForValue().get(actualKey);
                case "HGET" -> {
                    String field = config.getString("field");
                    yield redisTemplate.opsForHash().get(actualKey, field);
                }
                case "HGETALL" -> redisTemplate.opsForHash().entries(actualKey);
                case "ZSCORE" -> {
                    String member = config.getString("member");
                    yield redisTemplate.opsForZSet().score(actualKey, member);
                }
                case "ZREVRANGE" -> {
                    Long start = config.getLongValue("start");
                    Long end = config.getLongValue("end", -1L);
                    yield redisTemplate.opsForZSet().reverseRange(actualKey, start, end);
                }
                default -> {
                    log.warn("Unsupported Redis command: {}", command);
                    yield null;
                }
            };
        } catch (Exception e) {
            log.error("Redis command execution failed: command={}, key={}", command, actualKey, e);
            return null;
        }
    }

    private String replacePlaceholders(String pattern, Map<String, Object> params) {
        if (params == null || pattern == null) {
            return pattern;
        }
        
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(pattern);
        StringBuilder result = new StringBuilder();
        
        while (matcher.find()) {
            String paramName = matcher.group(1);
            Object value = params.get(paramName);
            matcher.appendReplacement(result, value != null ? String.valueOf(value) : "");
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    public SupplierType getSupplierType() {
        return SupplierType.REDIS;
    }
}
