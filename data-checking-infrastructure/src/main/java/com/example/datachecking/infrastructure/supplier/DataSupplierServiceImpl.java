package com.example.datachecking.infrastructure.supplier;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.example.datachecking.domain.model.SupplierType;
import com.example.datachecking.infrastructure.persistence.DataSupplierRepository;
import com.example.datachecking.infrastructure.persistence.entity.DataSupplierEntity;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSupplierServiceImpl implements com.example.datachecking.domain.service.DataSupplierService {

    private final DataSupplierRepository dataSupplierRepository;
    private final JdbcTemplate jdbcTemplate;
    private final RestTemplate restTemplate;

    private final Map<String, DataSupplierEntity> supplierCache = new ConcurrentHashMap<>();

    @Override
    public Object supply(String supplierKey, Object params) {
        DataSupplierEntity supplier = getSupplier(supplierKey);
        if (supplier == null) {
            log.warn("Data supplier not found: {}", supplierKey);
            return null;
        }

        SupplierType supplierType = SupplierType.valueOf(supplier.getSupplierType());
        return switch (supplierType) {
            case HTTP -> supplyHttp(supplier, params);
            case DB -> supplyDb(supplier, params);
        };
    }

    private DataSupplierEntity getSupplier(String supplierKey) {
        DataSupplierEntity cached = supplierCache.get(supplierKey);
        if (cached != null) {
            return cached;
        }

        synchronized (this) {
            cached = supplierCache.get(supplierKey);
            if (cached != null) {
                return cached;
            }

            DataSupplierEntity entity = dataSupplierRepository.selectOne(
                    new LambdaQueryWrapper<DataSupplierEntity>()
                            .eq(DataSupplierEntity::getSupplierKey, supplierKey)
                            .eq(DataSupplierEntity::getEnabled, true)
            );

            if (entity != null) {
                supplierCache.put(supplierKey, entity);
            }
            return entity;
        }
    }

    private Object supplyHttp(DataSupplierEntity supplier, Object params) {
        try {
            JSONObject config = JSON.parseObject(supplier.getConfig());
            String url = config.getString("url");
            String method = config.getString("method");
            Integer timeout = config.getInteger("timeout");

            Map<String, Object> requestBody = buildRequestBody(params, config);

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            if (config.containsKey("headers")) {
                JSONObject headersConfig = config.getJSONObject("headers");
                headersConfig.forEach((k, v) -> headers.add(k, String.valueOf(v)));
            }

            org.springframework.http.HttpEntity<Map<String, Object>> request =
                    new org.springframework.http.HttpEntity<>(requestBody, headers);

            var response = restTemplate.exchange(
                    url,
                    org.springframework.http.HttpMethod.valueOf(method),
                    request,
                    JSONObject.class
            );

            JSONObject responseBody = response.getBody();
            if (responseBody == null) {
                return null;
            }

            if (config.containsKey("responseMapping")) {
                return mapResponse(responseBody, config.getJSONObject("responseMapping"));
            }

            return responseBody;
        } catch (Exception e) {
            log.error("HTTP supplier call failed: supplierKey={}", supplier.getSupplierKey(), e);
            return null;
        }
    }

    private Map<String, Object> buildRequestBody(Object params, JSONObject config) {
        Map<String, Object> body = new java.util.HashMap<>();
        
        if (params instanceof Map) {
            body.putAll((Map<String, Object>) params);
        }

        if (config.containsKey("requestMapping")) {
            JSONObject mapping = config.getJSONObject("requestMapping");
            Map<String, Object> mappedBody = new java.util.HashMap<>();
            for (String key : mapping.keySet()) {
                String sourceKey = mapping.getString(key);
                if (body.containsKey(sourceKey)) {
                    mappedBody.put(key, body.get(sourceKey));
                }
            }
            return mappedBody;
        }

        return body;
    }

    private Object mapResponse(JSONObject response, JSONObject mapping) {
        Map<String, Object> result = new java.util.HashMap<>();
        for (String key : mapping.keySet()) {
            String path = mapping.getString(key);
            Object value = extractByPath(response, path);
            result.put(key, value);
        }
        return result;
    }

    private Object extractByPath(JSONObject json, String path) {
        String[] parts = path.split("\\.");
        Object current = json;
        for (String part : parts) {
            if (current instanceof JSONObject) {
                current = ((JSONObject) current).get(part);
            } else {
                return null;
            }
        }
        return current;
    }

    private Object supplyDb(DataSupplierEntity supplier, Object params) {
        try {
            JSONObject config = JSON.parseObject(supplier.getConfig());
            String sql = config.getString("sql");

            if (params instanceof Map) {
                Map<String, Object> paramMap = (Map<String, Object>) params;
                for (Map.Entry<String, Object> entry : paramMap.entrySet()) {
                    sql = sql.replace("#{" + entry.getKey() + "}", String.valueOf(entry.getValue()));
                }
            }

            return jdbcTemplate.queryForObject(sql, Object.class);
        } catch (Exception e) {
            log.error("DB supplier query failed: supplierKey={}", supplier.getSupplierKey(), e);
            return null;
        }
    }

    public void refreshCache() {
        supplierCache.clear();
        log.info("Data supplier cache cleared");
    }
}