# 特征查询服务实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现特征查询服务，支持根据 UID + 特征 Key 列表从多种数据源（HTTP/RPC/DB/REDIS）提取数据，经过 SpEL/Groovy 脚本清洗后返回

**Architecture:** 采用 DDD 六边形分层架构，扩展现有 DataSupplierService 支持多数据源，新增特征配置管理，实现脚本转换引擎

**Tech Stack:** Spring Boot 3.5.7, Dubbo 3.3.5, SpEL, Groovy, Caffeine, Redis

---

## 文件结构设计

```
data-checking-domain/src/main/java/com/example/datachecking/domain/
├── model/
│   ├── SupplierType.java          # 枚举扩展：HTTP/DB/RPC/REDIS
│   └── FeatureConfig.java         # 新增：特征配置领域模型

data-checking-api/src/main/java/com/example/datachecking/api/
├── dto/
│   ├── FeatureQueryRequest.java   # 新增
│   └── FeatureQueryResponse.java  # 新增
└── FeatureQueryService.java       # 新增：Dubbo 接口

data-checking-application/src/main/java/com/example/datachecking/application/
├── dto/
│   └── FeatureQueryCommand.java   # 新增
└── service/
    └── FeatureQueryApplicationService.java  # 新增

data-checking-infrastructure/src/main/java/com/example/datachecking/infrastructure/
├── supplier/
│   ├── DataSupplierServiceImpl.java      # 扩展：支持 RPC/REDIS
│   ├── RpcSupplierExecutor.java          # 新增：RPC 执行器
│   └── RedisSupplierExecutor.java        # 新增：Redis 执行器
├── engine/
│   ├── SpelTransformEngine.java           # 新增：SpEL 转换引擎
│   └── GroovyTransformEngine.java         # 新增：Groovy 转换引擎
├── cache/
│   ├── FeatureLocalCache.java             # 新增：Caffeine 本地缓存
│   └── FeatureRedisCache.java            # 新增：Redis 缓存
└── persistence/
    ├── entity/
    │   └── FeatureConfigEntity.java      # 新增
    ├── mapper/
    │   └── FeatureConfigMapper.java      # 新增
    └── repository/
        └── FeatureConfigRepositoryImpl.java  # 新增

data-checking-adapter/src/main/java/com/example/datachecking/adapter/dubbo/
└── FeatureQueryServiceImpl.java          # 新增
```

---

## Task 1: 领域模型 - SupplierType 枚举扩展

**Files:**
- Modify: `data-checking-domain/src/main/java/com/example/datachecking/domain/model/SupplierType.java`

- [ ] **Step 1: 扩展 SupplierType 枚举**

```java
package com.example.datachecking.domain.model;

public enum SupplierType {
    HTTP,
    DB,
    RPC,
    REDIS
}
```

- [ ] **Step 2: 提交**

```bash
git add data-checking-domain/src/main/java/com/example/datachecking/domain/model/SupplierType.java
git commit -m "feat: extend SupplierType with RPC and REDIS"
```

---

## Task 2: 领域模型 - FeatureConfig

**Files:**
- Create: `data-checking-domain/src/main/java/com/example/datachecking/domain/model/FeatureConfig.java`

- [ ] **Step 1: 创建 FeatureConfig 领域模型**

```java
package com.example.datachecking.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String featureKey;
    private String featureName;
    private SupplierType supplierType;
    private String supplierKey;
    private TransformType transformType;
    private String transformScript;
    private String extractPath;
    private Integer cacheTtl;
    private Integer priority;
    private Boolean enabled;

    public enum TransformType {
        NONE,
        SPEL,
        GROOVY
    }

    public boolean isCacheable() {
        return cacheTtl != null && cacheTtl != -1;
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add data-checking-domain/src/main/java/com/example/datachecking/domain/model/FeatureConfig.java
git commit -m "feat: add FeatureConfig domain model"
```

---

## Task 3: API - 请求响应 DTO

**Files:**
- Create: `data-checking-api/src/main/java/com/example/datachecking/api/dto/FeatureQueryRequest.java`
- Create: `data-checking-api/src/main/java/com/example/datachecking/api/dto/FeatureQueryResponse.java`

- [ ] **Step 1: 创建 FeatureQueryRequest**

```java
package com.example.datachecking.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureQueryRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long uid;
    private List<String> featureKeys;
    private Map<String, Object> params;
}
```

- [ ] **Step 2: 创建 FeatureQueryResponse**

```java
package com.example.datachecking.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureQueryResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Builder.Default
    private Map<String, Object> features = new HashMap<>();
    
    @Builder.Default
    private Map<String, String> sources = new HashMap<>();
    
    @Builder.Default
    private Map<String, Long> timestamps = new HashMap<>();
    
    private boolean success;
    private String errorMessage;
}
```

- [ ] **Step 3: 提交**

```bash
git add data-checking-api/src/main/java/com/example/datachecking/api/dto/FeatureQueryRequest.java
git add data-checking-api/src/main/java/com/example/datachecking/api/dto/FeatureQueryResponse.java
git commit -m "feat: add FeatureQueryRequest and FeatureQueryResponse DTOs"
```

---

## Task 4: API - Dubbo 接口定义

**Files:**
- Create: `data-checking-api/src/main/java/com/example/datachecking/api/FeatureQueryService.java`

- [ ] **Step 1: 创建 Dubbo 接口**

```java
package com.example.datachecking.api;

import com.example.datachecking.api.dto.FeatureQueryRequest;
import com.example.datachecking.api.dto.FeatureQueryResponse;

public interface FeatureQueryService {
    
    FeatureQueryResponse queryFeatures(FeatureQueryRequest request);
}
```

- [ ] **Step 2: 提交**

```bash
git add data-checking-api/src/main/java/com/example/datachecking/api/FeatureQueryService.java
git commit -m "feat: add FeatureQueryService Dubbo interface"
```

---

## Task 5: Infrastructure - FeatureConfigEntity 实体

**Files:**
- Create: `data-checking-infrastructure/src/main/java/com/example/datachecking/infrastructure/persistence/entity/FeatureConfigEntity.java`

- [ ] **Step 1: 创建实体**

```java
package com.example.datachecking.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_feature_config")
public class FeatureConfigEntity {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    private String featureKey;
    private String featureName;
    private String supplierType;
    private String supplierKey;
    private String transformType;
    private String transformScript;
    private String extractPath;
    private Integer cacheTtl;
    private Integer priority;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 2: 提交**

```bash
git add data-checking-infrastructure/src/main/java/com/example/datachecking/infrastructure/persistence/entity/FeatureConfigEntity.java
git commit -m "feat: add FeatureConfigEntity"
```

---

## Task 6: Infrastructure - FeatureConfigMapper

**Files:**
- Create: `data-checking-infrastructure/src/main/java/com/example/datachecking/infrastructure/persistence/mapper/FeatureConfigMapper.java`

- [ ] **Step 1: 创建 Mapper**

```java
package com.example.datachecking.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.datachecking.infrastructure.persistence.entity.FeatureConfigEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FeatureConfigMapper extends BaseMapper<FeatureConfigEntity> {
}
```

- [ ] **Step 2: 提交**

```bash
git add data-checking-infrastructure/src/main/java/com/example/datachecking/infrastructure/persistence/mapper/FeatureConfigMapper.java
git commit -m "feat: add FeatureConfigMapper"
```

---

## Task 7: Infrastructure - RedisSupplierExecutor

**Files:**
- Create: `data-checking-infrastructure/src/main/java/com/example/datachecking/infrastructure/supplier/RedisSupplierExecutor.java`

- [ ] **Step 1: 创建 RedisSupplierExecutor**

```java
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
                    Long start = config.getLong("start", 0L);
                    Long end = config.getLong("end", -1L);
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
```

- [ ] **Step 2: 提交**

```bash
git add data-checking-infrastructure/src/main/java/com/example/datachecking/infrastructure/supplier/RedisSupplierExecutor.java
git commit -m "feat: add RedisSupplierExecutor"
```

---

## Task 8: Infrastructure - RpcSupplierExecutor

**Files:**
- Create: `data-checking-infrastructure/src/main/java/com/example/datachecking/infrastructure/supplier/RpcSupplierExecutor.java`

- [ ] **Step 1: 创建 RpcSupplierExecutor**

```java
package com.example.datachecking.infrastructure.supplier;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.example.datachecking.domain.model.SupplierType;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.rpc.service.GenericService;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class RpcSupplierExecutor {

    private final ApplicationConfig applicationConfig;
    private final RegistryConfig registryConfig;
    private final Map<String, ReferenceConfig<GenericService>> referenceCache = new ConcurrentHashMap<>();

    public RpcSupplierExecutor(String applicationName, String registryAddress) {
        this.applicationConfig = new ApplicationConfig(applicationName);
        this.registryConfig = new RegistryConfig(registryAddress);
    }

    public Object execute(String configJson, Map<String, Object> params) {
        JSONObject config = JSON.parseObject(configJson);
        
        String interfaceName = config.getString("interface");
        String methodName = config.getString("method");
        String group = config.getString("group");
        String version = config.getString("version");
        JSONObject paramMapping = config.getJSONObject("paramMapping");
        
        try {
            ReferenceConfig<GenericService> ref = referenceCache.get(interfaceName + ":" + methodName);
            if (ref == null) {
                ref = new ReferenceConfig<>();
                ref.setApplication(applicationConfig);
                ref.setRegistry(registryConfig);
                ref.setInterface(interfaceName);
                ref.setGeneric(true);
                ref.setTimeout(3000);
                
                if (group != null) {
                    ref.setGroup(group);
                }
                if (version != null) {
                    ref.setVersion(version);
                }
                
                referenceCache.put(interfaceName + ":" + methodName, ref);
            }
            
            GenericService genericService = ref.get();
            Object[] args = buildRpcArgs(params, paramMapping);
            
            Class<?>[] paramTypes = args.length > 0 
                ? new Class[]{args[0].getClass()} 
                : new Class[]{};
            
            return genericService.$invoke(methodName, paramTypes, args);
            
        } catch (Exception e) {
            log.error("RPC call failed: interface={}, method={}", interfaceName, methodName, e);
            return null;
        }
    }

    private Object[] buildRpcArgs(Map<String, Object> params, JSONObject paramMapping) {
        if (paramMapping == null || params == null) {
            return params != null ? new Object[]{params} : new Object[0];
        }
        
        Object[] args = new Object[paramMapping.size()];
        int index = 0;
        for (String key : paramMapping.keySet()) {
            String paramName = paramMapping.getString(key);
            args[index++] = params.get(paramName);
        }
        return args;
    }
    
    public SupplierType getSupplierType() {
        return SupplierType.RPC;
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add data-checking-infrastructure/src/main/java/com/example/datachecking/infrastructure/supplier/RpcSupplierExecutor.java
git commit -m "feat: add RpcSupplierExecutor"
```

---

## Task 9: Infrastructure - DataSupplierService 扩展

**Files:**
- Modify: `data-checking-infrastructure/src/main/java/com/example/datachecking/infrastructure/supplier/DataSupplierServiceImpl.java`

- [ ] **Step 1: 注入新的执行器**

在 `DataSupplierServiceImpl` 类中添加：

```java
private final RedisSupplierExecutor redisSupplierExecutor;
private RpcSupplierExecutor rpcSupplierExecutor;
```

- [ ] **Step 2: 扩展 supply 方法**

```java
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
        case RPC -> supplyRpc(supplier, params);
        case REDIS -> supplyRedis(supplier, params);
    };
}

private Object supplyRpc(DataSupplierEntity supplier, Object params) {
    if (rpcSupplierExecutor == null) {
        rpcSupplierExecutor = new RpcSupplierExecutor(
            applicationName, 
            registryAddress
        );
    }
    return rpcSupplierExecutor.execute(supplier.getConfig(), (Map<String, Object>) params);
}

private Object supplyRedis(DataSupplierEntity supplier, Object params) {
    return redisSupplierExecutor.execute(supplier.getConfig(), (Map<String, Object>) params);
}
```

- [ ] **Step 3: 提交**

```bash
git add data-checking-infrastructure/src/main/java/com/example/datachecking/infrastructure/supplier/DataSupplierServiceImpl.java
git commit -m "feat: extend DataSupplierService with RPC and REDIS support"
```

---

## Task 10: Infrastructure - SpelTransformEngine

**Files:**
- Create: `data-checking-infrastructure/src/main/java/com/example/datachecking/infrastructure/engine/SpelTransformEngine.java`

- [ ] **Step 1: 创建 SpelTransformEngine**

```java
package com.example.datachecking.infrastructure.engine;

import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class SpelTransformEngine {

    private final SpelExpressionParser parser = new SpelExpressionParser();
    private final Map<String, Expression> expressionCache = new ConcurrentHashMap<>();

    public Object transform(String script, Object rootObject) {
        if (script == null || script.isEmpty()) {
            return rootObject;
        }
        
        try {
            Expression expression = expressionCache.get(script);
            if (expression == null) {
                expression = parser.parseExpression(script);
                expressionCache.put(script, expression);
            }
            
            StandardEvaluationContext context = new StandardEvaluationContext();
            context.setVariable("root", rootObject);
            
            if (rootObject instanceof Map) {
                Map<?, ?> rootMap = (Map<?, ?>) rootObject;
                for (Map.Entry<?, ?> entry : rootMap.entrySet()) {
                    context.setVariable(entry.getKey().toString(), entry.getValue());
                }
            }
            
            return expression.getValue(context);
            
        } catch (Exception e) {
            log.error("SpEL transform failed: script={}", script, e);
            return null;
        }
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add data-checking-infrastructure/src/main/java/com/example/datachecking/infrastructure/engine/SpelTransformEngine.java
git commit -m "feat: add SpelTransformEngine"
```

---

## Task 11: Infrastructure - GroovyTransformEngine

**Files:**
- Create: `data-checking-infrastructure/src/main/java/com/example/datachecking/infrastructure/engine/GroovyTransformEngine.java`

- [ ] **Step 1: 创建 GroovyTransformEngine**

```java
package com.example.datachecking.infrastructure.engine;

import lombok.extern.slf4j.Slf4j;
import org.codehaus.groovy.jsr223.GroovyScriptEngineFactory;
import org.springframework.stereotype.Component;

import javax.script.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class GroovyTransformEngine {

    private final GroovyScriptEngineFactory engineFactory = new GroovyScriptEngineFactory();
    private final Map<String, CompiledScript> scriptCache = new ConcurrentHashMap<>();
    private final ThreadLocal<ScriptEngine> engineThreadLocal = ThreadLocal.withInitial(() -> engineFactory.getScriptEngine());

    public Object transform(String script, Object rootObject) {
        if (script == null || script.isEmpty()) {
            return rootObject;
        }
        
        try {
            CompiledScript compiled = scriptCache.get(script);
            if (compiled == null) {
                ScriptEngine engine = engineThreadLocal.get();
                compiled = ((Compilable) engine).compile(script);
                scriptCache.put(script, compiled);
            }
            
            Bindings bindings = compiled.getEngine().createBindings();
            bindings.put("root", rootObject);
            
            if (rootObject instanceof Map) {
                Map<?, ?> rootMap = (Map<?, ?>) rootObject;
                for (Map.Entry<?, ?> entry : rootMap.entrySet()) {
                    bindings.put(entry.getKey().toString(), entry.getValue());
                }
            }
            
            return compiled.eval(bindings);
            
        } catch (Exception e) {
            log.error("Groovy transform failed: script={}", script, e);
            return null;
        }
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add data-checking-infrastructure/src/main/java/com/example/datachecking/infrastructure/engine/GroovyTransformEngine.java
git commit -m "feat: add GroovyTransformEngine"
```

---

## Task 12: Infrastructure - FeatureLocalCache

**Files:**
- Create: `data-checking-infrastructure/src/main/java/com/example/datachecking/infrastructure/cache/FeatureLocalCache.java`

- [ ] **Step 1: 创建 FeatureLocalCache**

```java
package com.example.datachecking.infrastructure.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class FeatureLocalCache {

    private final Cache<String, CacheEntry> cache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(10))
            .build();

    public void put(String key, Object value, int ttlSeconds) {
        cache.put(key, new CacheEntry(value, System.currentTimeMillis()));
    }

    public Object get(String key) {
        CacheEntry entry = cache.getIfPresent(key);
        if (entry == null) {
            return null;
        }
        return entry.value;
    }

    public void invalidate(String key) {
        cache.invalidate(key);
    }

    public void clear() {
        cache.invalidateAll();
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
```

- [ ] **Step 2: 提交**

```bash
git add data-checking-infrastructure/src/main/java/com/example/datachecking/infrastructure/cache/FeatureLocalCache.java
git commit -m "feat: add FeatureLocalCache with Caffeine"
```

---

## Task 13: Infrastructure - FeatureRedisCache

**Files:**
- Create: `data-checking-infrastructure/src/main/java/com/example/datachecking/infrastructure/cache/FeatureRedisCache.java`

- [ ] **Step 1: 创建 FeatureRedisCache**

```java
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
```

- [ ] **Step 2: 提交**

```bash
git add data-checking-infrastructure/src/main/java/com/example/datachecking/infrastructure/cache/FeatureRedisCache.java
git commit -m "feat: add FeatureRedisCache"
```

---

## Task 14: Application - FeatureQueryApplicationService

**Files:**
- Create: `data-checking-application/src/main/java/com/example/datachecking/application/service/FeatureQueryApplicationService.java`

- [ ] **Step 1: 创建 FeatureQueryApplicationService**

```java
package com.example.datachecking.application.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.example.datachecking.api.dto.FeatureQueryRequest;
import com.example.datachecking.api.dto.FeatureQueryResponse;
import com.example.datachecking.application.dto.FeatureQueryCommand;
import com.example.datachecking.domain.model.FeatureConfig;
import com.example.datachecking.domain.model.SupplierType;
import com.example.datachecking.domain.service.DataSupplierService;
import com.example.datachecking.infrastructure.cache.FeatureLocalCache;
import com.example.datachecking.infrastructure.cache.FeatureRedisCache;
import com.example.datachecking.infrastructure.engine.GroovyTransformEngine;
import com.example.datachecking.infrastructure.engine.SpelTransformEngine;
import com.example.datachecking.infrastructure.persistence.entity.FeatureConfigEntity;
import com.example.datachecking.infrastructure.persistence.mapper.FeatureConfigMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeatureQueryApplicationService {

    private final FeatureConfigMapper featureConfigMapper;
    private final DataSupplierService dataSupplierService;
    private final SpelTransformEngine spelTransformEngine;
    private final GroovyTransformEngine groovyTransformEngine;
    private final FeatureLocalCache localCache;
    private final FeatureRedisCache redisCache;

    public FeatureQueryResponse queryFeatures(FeatureQueryRequest request) {
        try {
            List<String> featureKeys = request.getFeatureKeys();
            Long uid = request.getUid();
            Map<String, Object> params = buildParams(uid, request.getParams());
            
            Map<String, Object> features = new HashMap<>();
            Map<String, String> sources = new HashMap<>();
            Map<String, Long> timestamps = new HashMap<>();
            
            Map<String, FeatureConfig> configMap = loadFeatureConfigs(featureKeys);
            
            Map<String, List<String>> supplierToFeatures = groupBySupplier(configMap);
            
            Map<String, Object> rawDataMap = fetchRawDataSuppliers(supplierToFeatures, params);
            
            for (String featureKey : featureKeys) {
                FeatureConfig config = configMap.get(featureKey);
                if (config == null || !config.getEnabled()) {
                    features.put(featureKey, null);
                    sources.put(featureKey, "NOT_FOUND");
                    continue;
                }
                
                Object rawValue = extractRawValue(config, rawDataMap);
                Object transformedValue = transformValue(config, rawValue);
                
                features.put(featureKey, transformedValue);
                sources.put(featureKey, config.getSupplierType().name());
                timestamps.put(featureKey, System.currentTimeMillis());
                
                if (config.isCacheable()) {
                    cacheFeature(uid, featureKey, transformedValue, config.getCacheTtl());
                }
            }
            
            return FeatureQueryResponse.builder()
                    .features(features)
                    .sources(sources)
                    .timestamps(timestamps)
                    .success(true)
                    .build();
                    
        } catch (Exception e) {
            log.error("Feature query failed: uid={}", request.getUid(), e);
            return FeatureQueryResponse.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    private Map<String, Object> buildParams(Long uid, Map<String, Object> extraParams) {
        Map<String, Object> params = new HashMap<>();
        if (uid != null) {
            params.put("uid", uid);
        }
        if (extraParams != null) {
            params.putAll(extraParams);
        }
        return params;
    }

    private Map<String, FeatureConfig> loadFeatureConfigs(List<String> featureKeys) {
        List<FeatureConfigEntity> entities = featureConfigMapper.selectList(
                new LambdaQueryWrapper<FeatureConfigEntity>()
                        .in(FeatureConfigEntity::getFeatureKey, featureKeys)
                        .eq(FeatureConfigEntity::getEnabled, true)
        );
        
        Map<String, FeatureConfig> configMap = new HashMap<>();
        for (FeatureConfigEntity entity : entities) {
            configMap.put(entity.getFeatureKey(), toFeatureConfig(entity));
        }
        return configMap;
    }

    private Map<String, List<String>> groupBySupplier(Map<String, FeatureConfig> configMap) {
        Map<String, List<String>> supplierToFeatures = new HashMap<>();
        for (Map.Entry<String, FeatureConfig> entry : configMap.entrySet()) {
            String featureKey = entry.getKey();
            FeatureConfig config = entry.getValue();
            supplierToFeatures
                    .computeIfAbsent(config.getSupplierKey(), k -> new ArrayList<>())
                    .add(featureKey);
        }
        return supplierToFeatures;
    }

    private Map<String, Object> fetchRawDataSuppliers(Map<String, List<String>> supplierToFeatures, Map<String, Object> params) {
        List<CompletableFuture<Map<String, Object>>> futures = supplierToFeatures.keySet()
                .stream()
                .map(supplierKey -> CompletableFuture.supplyAsync(() -> {
                    Object rawData = dataSupplierService.supply(supplierKey, params);
                    Map<String, Object> result = new HashMap<>();
                    for (String featureKey : supplierToFeatures.get(supplierKey)) {
                        result.put(featureKey, rawData);
                    }
                    return result;
                }))
                .collect(Collectors.toList());
        
        Map<String, Object> rawDataMap = new HashMap<>();
        for (CompletableFuture<Map<String, Object>> f : futures) {
            try {
                rawDataMap.putAll(f.get());
            } catch (Exception e) {
                log.error("Failed to fetch raw data", e);
            }
        }
        return rawDataMap;
    }

    private Object extractRawValue(FeatureConfig config, Map<String, Object> rawDataMap) {
        String extractPath = config.getExtractPath();
        if (extractPath == null || extractPath.isEmpty()) {
            return rawDataMap.get(config.getFeatureKey());
        }
        
        Object rawData = rawDataMap.get(config.getFeatureKey());
        if (rawData == null) {
            return null;
        }
        
        if (rawData instanceof JSONObject) {
            return extractByPath((JSONObject) rawData, extractPath);
        }
        return rawData;
    }

    private Object extractByPath(JSONObject json, String path) {
        String[] parts = path.split("\\.");
        Object current = json;
        for (String part : parts) {
            if (current instanceof JSONObject) {
                current = ((JSONObject) current).get(part);
            } else if (current instanceof com.alibaba.fastjson2.JSONArray) {
                try {
                    int idx = Integer.parseInt(part);
                    current = ((com.alibaba.fastjson2.JSONArray) current).get(idx);
                } catch (NumberFormatException e) {
                    return null;
                }
            } else {
                return null;
            }
        }
        return current;
    }

    private Object transformValue(FeatureConfig config, Object rawValue) {
        if (rawValue == null) {
            return null;
        }
        
        FeatureConfig.TransformType transformType = config.getTransformType();
        if (transformType == null || transformType == FeatureConfig.TransformType.NONE) {
            return rawValue;
        }
        
        String script = config.getTransformScript();
        return switch (transformType) {
            case SPEL -> spelTransformEngine.transform(script, rawValue);
            case GROOVY -> groovyTransformEngine.transform(script, rawValue);
            default -> rawValue;
        };
    }

    private void cacheFeature(Long uid, String featureKey, Object value, int ttlSeconds) {
        String cacheKey = uid + ":" + featureKey;
        localCache.put(cacheKey, value, ttlSeconds);
        redisCache.put(cacheKey, value, ttlSeconds);
    }

    private FeatureConfig toFeatureConfig(FeatureConfigEntity entity) {
        return FeatureConfig.builder()
                .id(entity.getId())
                .featureKey(entity.getFeatureKey())
                .featureName(entity.getFeatureName())
                .supplierType(SupplierType.valueOf(entity.getSupplierType()))
                .supplierKey(entity.getSupplierKey())
                .transformType(FeatureConfig.TransformType.valueOf(entity.getTransformType()))
                .transformScript(entity.getTransformScript())
                .extractPath(entity.getExtractPath())
                .cacheTtl(entity.getCacheTtl())
                .priority(entity.getPriority())
                .enabled(entity.getEnabled())
                .build();
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add data-checking-application/src/main/java/com/example/datachecking/application/service/FeatureQueryApplicationService.java
git commit -m "feat: add FeatureQueryApplicationService"
```

---

## Task 15: Adapter - FeatureQueryServiceImpl

**Files:**
- Create: `data-checking-adapter/src/main/java/com/example/datachecking/adapter/dubbo/FeatureQueryServiceImpl.java`

- [ ] **Step 1: 创建 FeatureQueryServiceImpl**

```java
package com.example.datachecking.adapter.dubbo;

import com.example.datachecking.api.FeatureQueryService;
import com.example.datachecking.api.dto.FeatureQueryRequest;
import com.example.datachecking.api.dto.FeatureQueryResponse;
import com.example.datachecking.application.service.FeatureQueryApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

@Slf4j
@DubboService(version = "1.0.0", group = "data-checking", timeout = 5000)
@RequiredArgsConstructor
public class FeatureQueryServiceImpl implements FeatureQueryService {

    private final FeatureQueryApplicationService featureQueryApplicationService;

    @Override
    public FeatureQueryResponse queryFeatures(FeatureQueryRequest request) {
        log.info("Feature query request: uid={}, keys={}", request.getUid(), request.getFeatureKeys());
        return featureQueryApplicationService.queryFeatures(request);
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add data-checking-adapter/src/main/java/com/example/datachecking/adapter/dubbo/FeatureQueryServiceImpl.java
git commit -m "feat: add FeatureQueryServiceImpl Dubbo service"
```

---

## Task 16: 数据库脚本

**Files:**
- Create: `docs/superpowers/plans/t_feature_config.sql`

- [ ] **Step 1: 创建建表脚本**

```sql
CREATE TABLE t_feature_config (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    feature_key    VARCHAR(64) NOT NULL UNIQUE COMMENT '特征唯一标识',
    feature_name   VARCHAR(128) COMMENT '特征名称',
    supplier_type  VARCHAR(16) NOT NULL COMMENT '数据源类型: HTTP/RPC/DB/REDIS',
    supplier_key   VARCHAR(64) NOT NULL COMMENT '关联的DataSupplier key',
    transform_type VARCHAR(16) DEFAULT 'NONE' COMMENT '转换类型: NONE/SPEL/GROOVY',
    transform_script TEXT COMMENT '清洗脚本',
    extract_path   VARCHAR(128) COMMENT 'JSON提取路径',
    cache_ttl      INT DEFAULT -1 COMMENT '缓存TTL(秒): -1不缓存, 0永久, >0按TTL',
    priority       INT DEFAULT 0 COMMENT '优先级',
    enabled        TINYINT DEFAULT 1 COMMENT '是否启用',
    created_at     DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_feature_key (feature_key),
    INDEX idx_enabled (enabled)
) COMMENT '特征配置表';
```

- [ ] **Step 2: 提交**

```bash
git add docs/superpowers/plans/t_feature_config.sql
git commit -m "docs: add t_feature_config SQL script"
```

---

## 实施检查清单

- [ ] Task 1: SupplierType 枚举扩展
- [ ] Task 2: FeatureConfig 领域模型
- [ ] Task 3: 请求响应 DTO
- [ ] Task 4: Dubbo 接口定义
- [ ] Task 5: FeatureConfigEntity 实体
- [ ] Task 6: FeatureConfigMapper
- [ ] Task 7: RedisSupplierExecutor
- [ ] Task 8: RpcSupplierExecutor
- [ ] Task 9: DataSupplierService 扩展
- [ ] Task 10: SpelTransformEngine
- [ ] Task 11: GroovyTransformEngine
- [ ] Task 12: FeatureLocalCache
- [ ] Task 13: FeatureRedisCache
- [ ] Task 14: FeatureQueryApplicationService
- [ ] Task 15: FeatureQueryServiceImpl
- [ ] Task 16: 数据库脚本
