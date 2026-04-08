# 特征查询服务设计方案

## 一、需求概述

根据用户 UID + 特征 Key 列表，从多种数据源（HTTP、RPC、DB、Redis）提取数据，经过 SpEL/Groovy 脚本清洗加工后返回特征值。

### 调用方式
- Dubbo RPC 暴露给其他服务调用
- 高性能要求：P99 < 50ms
- 支持动态配置

---

## 二、接口设计

### 2.1 请求/响应结构

```java
// 请求
FeatureQueryRequest {
    Long uid;
    List<String> featureKeys;           // e.g. ["user_level", "credit_score", "tags"]
    Map<String, Object> params;         // 可选，传给数据源的额外参数
}

// 响应
FeatureQueryResponse {
    Map<String, Object> features;        // key -> value
    Map<String, String> sources;         // key -> 数据源类型
    Map<String, Long> timestamps;       // key -> 数据获取时间戳
}
```

### 2.2 Dubbo 接口定义

```java
@DubboService(version = "1.0.0", group = "data-checking")
public interface FeatureQueryService {
    FeatureQueryResponse queryFeatures(FeatureQueryRequest request);
}
```

---

## 三、数据模型

### 3.1 特征配置表 (t_feature_config)

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

### 3.2 数据源配置表扩展 (t_data_supplier)

已有表扩展，新增支持 RPC 和 Redis 类型：

| supplier_type | config 示例 |
|---------------|-------------|
| HTTP | `{"url":"http://api/user","method":"POST","requestMapping":{...},"responseMapping":{...}}` |
| DB | `{"sql":"SELECT level FROM t_user WHERE uid=#{uid}"}` |
| RPC | `{"interface":"com.xxx.UserService","method":"getUser","group":"xxx","version":"1.0","paramMapping":{...}}` |
| REDIS | `{"command":"GET","key":"user:{uid}:profile"}` |

### 3.3 SupplierType 枚举扩展

```java
public enum SupplierType {
    HTTP,   // 已有
    DB,     // 已有
    RPC,    // 新增：Dubbo 泛化调用
    REDIS   // 新增：Redis 查询
}
```

---

## 四、核心处理流程

### 4.1 Pipeline 架构

```
FeatureQueryRequest (uid=123, keys=[A,B,C])
    │
    ▼
① 特征配置解析
    ├─▶ FeatureA: supplierKey=X, transform=SpEL
    ├─▶ FeatureB: supplierKey=Y, transform=Groovy
    └─▶ FeatureC: supplierKey=X, transform=null
    ▼
② 数据源分组 (去重) ─── 避免重复请求相同数据源
    ├── Group1: supplierKey=X → [FeatureA, FeatureC]
    └── Group2: supplierKey=Y → [FeatureB]
    ▼
③ 并发拉取数据源
    ├── Group1: 调用 X 获取数据 → RawData1
    └── Group2: 调用 Y 获取数据 → RawData2
    ▼
④ 数据清洗加工 (SpEL/Groovy)
    ├── FeatureA: RawData1 + SpEL → value
    ├── FeatureB: RawData2 + Groovy → value
    └── FeatureC: RawData1 直接取 → value
    ▼
⑤ 缓存 + 返回结果
```

### 4.2 数据源分组去重

```java
Map<String, List<String>> supplierToFeatures = new HashMap<>();
for (String key : request.getFeatureKeys()) {
    FeatureConfig config = getConfig(key);
    supplierToFeatures
        .computeIfAbsent(config.getSupplierKey(), k -> new ArrayList<>())
        .add(key);
}

// 并发拉取（每个 supplierKey 只调用一次）
List<CompletableFuture<Map<String, Object>>> futures = supplierToFeatures.entrySet()
    .stream()
    .map(entry -> CompletableFuture.supplyAsync(() ->
        dataSupplierService.supply(entry.getKey(), params)
    ))
    .collect(Collectors.toList());

Map<String, Object> rawDataMap = new HashMap<>();
for (CompletableFuture<Map<String, Object>> f : futures) {
    rawDataMap.putAll(f.get());
}
```

---

## 五、SpEL/Groovy 脚本引擎

### 5.1 SpEL 脚本

**用途**: 简单字段提取、格式转换

**配置示例**:
```json
{
  "feature_key": "user_level",
  "supplier_type": "HTTP",
  "supplier_key": "user-api",
  "transform_type": "SPEL",
  "transform_script": "#root.data.user.level",
  "extract_path": "userInfo",
  "cache_ttl": 300
}
```

**实现**:
```java
public Object executeSpel(String script, Object rootObject) {
    Expression expression = spelCache.get(script);
    if (expression == null) {
        expression = parser.parseExpression(script);
        spelCache.put(script, expression);
    }
    return expression.getValue(rootObject);
}
```

### 5.2 Groovy 脚本

**用途**: 复杂计算、多字段运算、条件判断

**配置示例**:
```json
{
  "feature_key": "credit_score",
  "supplier_type": "DB",
  "supplier_key": "credit-query",
  "transform_type": "GROOVY",
  "transform_script": "def base = root.baseScore; def bonus = root.bonus; return base + bonus * 0.5",
  "cache_ttl": 3600
}
```

**实现**:
```java
public Object executeGroovy(String script, Object rootObject) {
    CompiledScript compiled = groovyCache.get(script);
    if (compiled == null) {
        compiled = new GroovyClassLoader().parseScript(script);
        groovyCache.put(script, compiled);
    }
    Binding binding = new Binding();
    binding.setVariable("root", rootObject);
    return compiled.execute(binding);
}
```

---

## 六、多数据源实现

### 6.1 HTTP 数据源

```java
private Object supplyHttp(DataSupplierEntity supplier, Object params) {
    JSONObject config = JSON.parseObject(supplier.getConfig());
    String url = config.getString("url");
    String method = config.getString("method", "POST");
    
    HttpHeaders headers = buildHeaders(config.getJSONObject("headers"));
    Map<String, Object> body = buildRequestBody(params, config.getJSONObject("requestMapping"));
    
    ResponseEntity<JSONObject> response = restTemplate.exchange(
        url, HttpMethod.valueOf(method),
        new HttpEntity<>(body, headers),
        JSONObject.class
    );
    
    return extractByPath(response.getBody(), config.getString("responsePath"));
}
```

### 6.2 Dubbo RPC 数据源 (新增)

```java
private Object supplyRpc(DataSupplierEntity supplier, Object params) {
    JSONObject config = JSON.parseObject(supplier.getConfig());
    
    String interfaceName = config.getString("interface");
    String methodName = config.getString("method");
    String group = config.getString("group");
    String version = config.getString("version");
    
    ReferenceConfig<GenericService> ref = referenceCache.get(supplier.getSupplierKey());
    if (ref == null) {
        ref = new ReferenceConfig<>();
        ref.setInterface(interfaceName);
        ref.setGeneric(true);
        if (group != null) ref.setGroup(group);
        if (version != null) ref.setVersion(version);
        ref.setUrl(dubboRegistryUrl);
        referenceCache.put(supplier.getSupplierKey(), ref);
    }
    
    GenericService service = ref.get();
    Object[] args = buildRpcArgs(params, config.getJSONObject("paramMapping"));
    return service.$invoke(methodName, new Class[]{args[0].getClass()}, args);
}
```

### 6.3 Redis 数据源 (新增)

```java
private Object supplyRedis(DataSupplierEntity supplier, Object params) {
    JSONObject config = JSON.parseObject(supplier.getConfig());
    
    String command = config.getString("command");
    String keyPattern = config.getString("key");
    String actualKey = replacePlaceholders(keyPattern, params);
    
    return switch (command.toUpperCase()) {
        case "GET" -> redisTemplate.opsForValue().get(actualKey);
        case "HGET" -> redisTemplate.opsForHash().get(actualKey, config.getString("field"));
        case "ZSCORE" -> redisTemplate.opsForZSet().score(actualKey, config.getString("member"));
        default -> null;
    };
}
```

### 6.4 DB 数据源 (扩展)

```java
private Object supplyDb(DataSupplierEntity supplier, Object params) {
    JSONObject config = JSON.parseObject(supplier.getConfig());
    String sql = config.getString("sql");
    
    for (Map.Entry<String, Object> entry : ((Map<String, Object>) params).entrySet()) {
        sql = sql.replace("#{" + entry.getKey() + "}", String.valueOf(entry.getValue()));
    }
    
    if (config.getBoolean("multiRow", false)) {
        return jdbcTemplate.queryForList(sql, Map.class);
    }
    return jdbcTemplate.queryForObject(sql, Map.class);
}
```

---

## 七、缓存策略

### 7.1 三级缓存

| 层级 | 存储 | 容量 | 淘汰策略 | 适用场景 |
|------|------|------|----------|----------|
| L1 | 本地 Caffeine | 1000 | LRU | 热点特征 |
| L2 | Redis | 无限制 | TTL 按配置 | 通用缓存 |
| L3 | 数据源 | - | - | 回源 |

### 7.2 缓存键设计

```
Redis: feature:{uid}:{featureKey}
TTL: 按 featureConfig.cacheTtl 配置
```

### 7.3 缓存判断

```java
public boolean isCacheable(FeatureConfig config) {
    return config.getCacheTtl() != -1;
}
```

---

## 八、模块设计

### 8.1 分层结构

| 模块 | 职责 |
|------|------|
| `domain` | FeatureConfig 领域模型、仓储接口 |
| `api` | FeatureQueryRequest/Response、 Dubbo 接口定义 |
| `application` | FeatureQueryApplicationService、批处理编排 |
| `infrastructure` | DataSupplierService 扩展、多数据源实现、脚本引擎 |
| `adapter` | Dubbo 服务实现 |

### 8.2 核心类

```
domain/model/
├── FeatureConfig.java           // 特征配置实体
├── SupplierType.java            // 枚举：HTTP/DB/RPC/REDIS

api/dto/
├── FeatureQueryRequest.java
├── FeatureQueryResponse.java
├── FeatureQueryService.java     // Dubbo 接口

application/service/
├── FeatureQueryApplicationService.java

infrastructure/
├── supplier/
│   ├── DataSupplierServiceImpl.java   // 扩展支持 RPC/REDIS
│   ├── RpcSupplierExecutor.java       // RPC 执行器
│   ├── RedisSupplierExecutor.java    // Redis 执行器
├── engine/
│   ├── SpelTransformEngine.java       // SpEL 转换引擎
│   ├── GroovyTransformEngine.java     // Groovy 转换引擎
├── cache/
│   ├── FeatureLocalCache.java         // Caffeine 本地缓存
│   └── FeatureRedisCache.java         // Redis 缓存

adapter/dubbo/
└── FeatureQueryServiceImpl.java       // Dubbo 服务实现
```

---

## 九、性能设计

### 9.1 优化手段

- **数据源去重**: 相同 supplierKey 只请求一次
- **并发拉取**: parallelStream 并发请求多个数据源
- **连接池**: HTTP、RPC、Redis 均使用连接池
- **脚本预编译**: SpEL/Groovy 脚本预编译缓存
- **本地缓存**: Caffeine 缓存热点特征

### 9.2 监控指标

| 指标名 | 类型 | 说明 |
|--------|------|------|
| `feature_query_total` | Counter | 查询总数 |
| `feature_query_duration` | Timer | 查询耗时 |
| `feature_cache_hit` | Counter | 缓存命中 |
| `feature_source_call` | Counter | 数据源调用次数 |

---

## 十、异常处理

| 场景 | 处理方式 |
|------|----------|
| 数据源调用失败 | 返回 null，特征值记为 null |
| 脚本执行失败 | 记录日志，返回 null |
| 配置缺失 | 抛出异常，提示配置错误 |
| 超时 | 设置超时，超时则返回 null |

---

## 十一、待确认事项

1. Dubbo 注册中心地址配置
2. 特征配置的 CRUD 接口是否需要
3. 是否需要特征变更通知机制
