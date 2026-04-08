# 数据实时核对系统 — 技术设计方案

## 一、系统定位与架构总览

### 1.1 系统定位

| 维度 | 说明 |
|------|------|
| **输入** | Kafka 消息（方法名 + 入参 + 返回值 JSON） |
| **处理** | 按规则引擎（SpEL/Groovy）实时核验数据 |
| **输出** | 指标上报 Prometheus + 失败数据落库 + 后台人工确认 |
| **核心约束** | 高吞吐（10万+ msg/s）、低延迟（P99 < 50ms）、规则热更新 |

### 1.2 整体架构

```
业务系统 ──▶ Kafka ──▶ 核对服务 ──┬──▶ Prometheus ──▶ Grafana 看板
                                ├──▶ MySQL (失败数据 + 规则配置)
                                └──▶ 后台管理 (规则配置 / 人工确认)
```

### 1.3 服务分层

```
┌─────────────────────────────────────────────────────┐
│                    接入层                            │
│  Kafka Consumer (批量消费 / 并行拉取)                  │
├─────────────────────────────────────────────────────┤
│                    规则层                            │
│  规则匹配器 (方法名通配 / 正则匹配)                     │
│  规则缓存 (Redis + 本地 LRU)                         │
├─────────────────────────────────────────────────────┤
│                    执行层                            │
│  SpEL 引擎 (预编译 + 对象池)                          │
│  Groovy 引擎 (预编译 + ClassLoader 隔离)              │
├─────────────────────────────────────────────────────┤
│                    输出层                            │
│  指标上报 (Micrometer / Counter / Timer)             │
│  失败落库 (异步批量写入 / 降级本地文件)                  │
└─────────────────────────────────────────────────────┘
```

---

## 二、核心技术流程

### 2.1 消息处理主流程

```
Kafka 消息到达
    │
    ▼
① JSON 反序列化 ──────────────────────── FastJSON2 / Jackson
    │
    ▼
② 规则匹配 ──────────────────────────── 方法名 → 规则列表 (O(1) 本地缓存)
    │
    ├─ 无匹配规则 → 跳过 (记录指标)
    │
    ▼
③ 获取预编译表达式 ──────────────────── ConcurrentHashMap 缓存命中
    │
    ├─ 未命中 → 编译 → 存入缓存 → 执行
    │
    ▼
④ 构建执行上下文 ────────────────────── 对象池复用 StandardEvaluationContext
    │
    ▼
⑤ 执行核验 ──────────────────────────── SpEL: expression.getValue(ctx, Boolean)
    │                                   Groovy: compiledScript.run(binding)
    ▼
⑥ 结果处理 ──────────────────────────── 成功 → 指标 +1
    │                                   失败 → 指标 +1 + 写入异步队列
    ▼
⑦ 批量 ack ──────────────────────────── 手动提交 offset
```

### 2.2 规则匹配流程

```
消息 method = "orderService.createOrder"
    │
    ▼
① 精确匹配 ──────────────────────────── "orderService.createOrder" → 规则列表
    │
    ▼
② 通配匹配 ──────────────────────────── "orderService.*" → 规则列表
    │
    ▼
③ 正则匹配 ──────────────────────────── "order.*\..*" → 规则列表
    │
    ▼
④ 合并去重 ──────────────────────────── 按优先级排序
```

**匹配数据结构：**

```
规则索引：
┌─────────────────────────────────────────────────────┐
│  exactMatch: Map<String, List<Rule>>                │
│  ├── "orderService.createOrder" → [rule1, rule2]    │
│  └── "paymentService.pay" → [rule3]                 │
│                                                      │
│  wildcardMatch: Map<String, List<Rule>>             │
│  ├── "orderService.*" → [rule4]                     │
│  └── "paymentService.*" → [rule5]                   │
│                                                      │
│  regexMatch: List<RegexRule>                        │
│  └── Pattern("order.*\..*") → [rule6]               │
└─────────────────────────────────────────────────────┘
```

### 2.3 预编译缓存流程

```
规则 ruleId=100, expression="#amount > 0"
    │
    ▼
① 查本地缓存 ──────────────────────── spelCache.get(100)
    │
    ├─ 命中 → 直接返回预编译对象
    │
    ├─ 未命中 → 编译 ──────────────── SpelExpressionParser.parseRaw(expr)
    │                                SpelCompileMode.IMMEDIATE
    │
    ▼
② 检查缓存容量 ────────────────────── 超过阈值 → LRU 淘汰冷门规则
    │
    ▼
③ 写入缓存 ────────────────────────── spelCache.put(100, compiledExpr)
    │
    ▼
④ 记录访问热度 ────────────────────── accessCounter.incrementAndGet(100)
```

**缓存层级：**

```
L1 本地缓存 (ConcurrentHashMap)
├── 容量: 1000 条
├── 淘汰: 访问热度 LRU
├── 命中延迟: < 1μs
└── 适用: 热点规则 (99% 场景)

L2 Redis 缓存
├── 容量: 全量规则
├── 淘汰: LRU (TTL 24h)
├── 命中延迟: < 5ms
└── 适用: 冷规则 / 多实例共享

L3 MySQL 持久化
├── 容量: 无限制
├── 淘汰: 无
├── 命中延迟: < 20ms
└── 适用: 规则持久化 / 审计
```

### 2.4 失败数据异步写入流程

```
核验失败
    │
    ▼
① 构建 CheckRecord ────────────────── 包含 traceId, 规则, 入参, 返回值, 失败原因
    │
    ▼
② 写入内存队列 ────────────────────── LinkedBlockingQueue (容量 5000)
    │
    ▼
③ 异步批量消费 ────────────────────── 定时任务 / 阈值触发 (100条 或 50ms)
    │
    ▼
④ 批量 INSERT ─────────────────────── MyBatis batch insert
    │
    ├─ 成功 → 清空队列
    │
    ├─ 失败 → 重试 3 次
    │
    └─ 仍失败 → 降级写本地文件 ────── 按日期分文件, 定时补偿
```

---

## 三、技术选型综合分析

### 3.1 核心组件选型

| 组件 | 候选方案 | 最终选择 | 决策依据 |
|------|----------|----------|----------|
| **消息队列** | Kafka / RocketMQ / RabbitMQ | **Kafka** | 吞吐最高、顺序消费、生态成熟 |
| **脚本引擎** | SpEL / Groovy / QLExpress / Aviator | **SpEL + Groovy** | SpEL 轻量适合简单规则，Groovy 灵活适合复杂逻辑 |
| **对象池** | 手写 / Commons Pool2 / Caffeine | **Commons Pool2** | 生产成熟、配置灵活、监控完善 |
| **JSON 解析** | Jackson / FastJSON2 / Gson | **FastJSON2** | 解析速度最快、内存占用低 |
| **ORM** | MyBatis / JPA / MyBatis-Plus | **MyBatis-Plus** | 批量写入性能好、开发效率高 |
| **缓存** | Redis / Caffeine / 本地 Map | **Redis + 本地双层** | Redis 分布式共享 + 本地零延迟 |
| **监控** | Micrometer / 自研 | **Micrometer + Prometheus** | Spring Boot 原生集成、Grafana 生态 |
| **序列化** | JSON / Protobuf / Avro | **JSON** | 可读性好、调试方便，性能足够 |

### 3.2 SpEL vs Groovy 选型矩阵

| 规则类型 | 推荐引擎 | 理由 | 示例场景 |
|----------|----------|------|----------|
| 简单条件判断 | **SpEL** | 编译快、内存小、语法简洁 | `#amount > 0 && #status == 'SUCCESS'` |
| 复杂业务逻辑 | **Groovy** | 支持完整 Java 语法、可写循环/分支 | 多层嵌套判断、日期计算、集合操作 |
| 正则匹配 | **SpEL** | 内置 matches() 函数 | `#phone matches '\\d{11}'` |
| 集合聚合 | **Groovy** | 集合操作符丰富 | `items.findAll { it.price > 0 }.sum()` |
| 外部 API 调用 | **Groovy** | 可注入 Spring Bean | `@Autowired OrderService.query()` |

### 3.3 性能优化技术栈

```
┌────────────────────────────────────────────────────────────┐
│                    性能优化全景                              │
├────────────────────────────────────────────────────────────┤
│                                                            │
│  编译层:  SpEL 预编译 (SpelCompileMode.IMMEDIATE)           │
│           Groovy 预编译 (GroovyClassLoader.parseClass)      │
│                                                            │
│  缓存层:  三级缓存 (本地 ConcurrentHashMap → Redis → MySQL) │
│           热点规则启动预热                                   │
│           规则变更自动刷新                                   │
│                                                            │
│  对象层:  Commons Pool2 对象池                              │
│           StandardEvaluationContext 复用                    │
│           Groovy Binding 复用 (ThreadLocal)                 │
│                                                            │
│  并发层:  Kafka 批量消费 (batch-listener)                   │
│           并行流处理 (parallelStream)                       │
│           异步写入 (CompletableFuture)                      │
│           线程池隔离 (核对线程池 / 写入线程池)                │
│                                                            │
│  IO 层:   MyBatis 批量 INSERT                               │
│           降级本地文件 (故障时)                              │
│           Kafka 手动 ack (处理完再提交)                      │
│                                                            │
└────────────────────────────────────────────────────────────┘
```

---

## 四、核心数据结构

### 4.1 Kafka 消息体

```json
{
  "traceId": "trace-abc123",
  "methodName": "orderService.createOrder",
  "timestamp": 1712102400000,
  "inputParams": {
    "userId": 10001,
    "amount": 99.9,
    "items": [
      {"skuId": "SKU001", "quantity": 2, "price": 49.95}
    ]
  },
  "returnData": {
    "code": "SUCCESS",
    "data": {
      "orderId": "ORD-20240403-001",
      "status": "CREATED",
      "totalAmount": 99.9
    }
  },
  "contextData": {
    "appId": "order-center",
    "env": "prod"
  }
}
```

### 4.2 核对规则表 (t_check_rule)

```sql
CREATE TABLE t_check_rule (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    rule_name       VARCHAR(128) NOT NULL COMMENT '规则名称',
    method_pattern  VARCHAR(256) NOT NULL COMMENT '方法匹配模式',
    match_type      TINYINT NOT NULL DEFAULT 1 COMMENT '匹配类型: 1-精确 2-通配 3-正则',
    rule_type       VARCHAR(16) NOT NULL COMMENT '引擎类型: SPEL / GROOVY',
    expression      TEXT NOT NULL COMMENT '核验表达式/脚本',
    priority        INT NOT NULL DEFAULT 0 COMMENT '优先级(越大越先执行)',
    enabled         TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
    version         INT NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_method_pattern (method_pattern),
    INDEX idx_enabled_priority (enabled, priority DESC)
) COMMENT '核对规则配置表';
```

### 4.3 核对结果表 (t_check_record)

```sql
CREATE TABLE t_check_record (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    trace_id        VARCHAR(64) NOT NULL COMMENT '链路追踪ID',
    rule_id         BIGINT NOT NULL COMMENT '匹配的规则ID',
    method_name     VARCHAR(256) NOT NULL COMMENT '方法名',
    check_result    VARCHAR(8) NOT NULL COMMENT 'PASS / FAIL',
    expression      TEXT COMMENT '执行的表达式(快照)',
    input_params    MEDIUMTEXT COMMENT '入参JSON',
    return_data     MEDIUMTEXT COMMENT '返回值JSON',
    fail_reason     VARCHAR(512) COMMENT '失败原因',
    confirm_status  TINYINT NOT NULL DEFAULT 0 COMMENT '0-待确认 1-已确认正常 2-已确认异常',
    confirm_user    VARCHAR(64) COMMENT '确认人',
    confirm_remark  VARCHAR(512) COMMENT '确认备注',
    confirm_at      DATETIME COMMENT '确认时间',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_trace_id (trace_id),
    INDEX idx_rule_id (rule_id),
    INDEX idx_confirm_status (confirm_status),
    INDEX idx_created_at (created_at)
) COMMENT '核对结果记录表';
```

> **分表建议：** 数据量大时对 `t_check_record` 按 `created_at` 月分表，或使用 ClickHouse 替代 MySQL 做分析查询。

### 4.4 预编译缓存结构

```
CompiledExpressionCache
├── spelCache: ConcurrentHashMap<Long, SpelExpression>
│   ├── key: ruleId
│   └── value: 预编译后的 SpEL 表达式对象
│
├── groovyCache: ConcurrentHashMap<Long, CompiledScript>
│   ├── key: ruleId
│   └── value: 预编译后的 Groovy Script 对象
│
├── accessCounter: ConcurrentHashMap<Long, AtomicLong>
│   └── 访问热度统计 (用于 LRU 淘汰)
│
└── 淘汰策略:
    └── 缓存满时 → 淘汰访问次数低于平均值的规则
```

### 4.5 规则索引结构

```
RuleIndex
├── exactMatch: ConcurrentHashMap<String, List<Rule>>
│   └── "orderService.createOrder" → [Rule1, Rule2]
│
├── wildcardMatch: ConcurrentHashMap<String, List<Rule>>
│   └── "orderService.*" → [Rule3]
│
├── regexMatch: List<CompiledRegexRule>
│   └── [Pattern, List<Rule>]
│
└── 匹配顺序: exact → wildcard → regex (短路优化)
```

---

## 五、核心实现要点

### 5.1 SpEL 预编译 + 对象池

```java
// 核心思路
// 1. SpEL 表达式只编译一次，缓存复用
// 2. StandardEvaluationContext 通过 Commons Pool2 复用
// 3. 变量绑定后执行，执行后快速清空归还

SpelExpression expr = cache.getCompiled(ruleId, expression);  // 预编译缓存
StandardEvaluationContext ctx = contextPool.borrowObject();    // 池化复用
try {
    bindVariables(ctx, message);                               // 快速绑定
    Boolean result = expr.getValue(ctx, Boolean.class);        // 执行
} finally {
    contextPool.returnObject(ctx);                             // 归还池
}
```

**关键性能点：**
- `SpelCompileMode.IMMEDIATE` 强制编译为字节码，而非解释执行
- Context 池化避免每次 `new StandardEvaluationContext()` 的内存分配
- 变量绑定直接 `setVariable`，避免反射查找

### 5.2 Groovy 预编译 + ClassLoader 隔离

```java
// 核心思路
// 1. Groovy 脚本编译为 Class 对象缓存
// 2. 每个脚本独立 ClassLoader，避免类冲突
// 3. 执行时通过 Binding 传参

GroovyClassLoader loader = new GroovyClassLoader();
Class<?> scriptClass = loader.parseClass(script);  // 预编译
GroovyObject instance = (GroovyObject) scriptClass.newInstance();
instance.setProperty("inputParams", message.getInputParams());
instance.setProperty("returnData", message.getReturnData());
Object result = instance.invokeMethod("run", null);
```

**关键性能点：**
- 预编译避免每次 `GroovyShell.evaluate()` 的解析开销
- 独立 ClassLoader 防止脚本间类名冲突
- 脚本中可注入 Spring Bean 实现复杂业务逻辑

### 5.3 Kafka 批量消费 + 并行处理

```java
// 核心思路
// 1. Kafka 配置批量拉取 (max.poll.records=500)
// 2. 批量消息并行处理 (parallelStream / 线程池)
// 3. 全部处理完再 ack

@KafkaListener(batchListener = true)
public void consume(List<String> messages) {
    // 并行处理
    List<CheckResult> results = messages.parallelStream()
        .map(this::processMessage)
        .collect(Collectors.toList());
    
    // 批量统计 + 异步写入
    metricsService.recordBatch(results);
    failWriter.writeBatchAsync(filterFailed(results));
    
    // 自动 ack (处理完整个批次)
}
```

**关键性能点：**
- 批量消费减少 Kafka 网络往返
- `parallelStream` 利用多核并行
- 线程池隔离：核对线程池 vs 写入线程池

### 5.4 规则热更新

```java
// 核心思路
// 1. 规则变更通过 Kafka topic 广播
// 2. 各实例监听变更事件
// 3. 增量刷新预编译缓存

@KafkaListener(topics = "rule-config-changes")
public void onRuleChange(RuleChangeEvent event) {
    if (event.isUpdate()) {
        cache.invalidate(event.getRuleId());        // 清除旧缓存
        cache.precompile(event.getNewRule());       // 预编译新规则
    } else if (event.isBatchRefresh()) {
        cache.invalidateAll();                       // 全量刷新
        ruleIndex.rebuild();                         // 重建索引
    }
}
```

### 5.5 异步失败写入 + 降级

```
AsyncFailWriter:
├── queue: LinkedBlockingQueue<CheckRecord> (容量 5000)
├── 触发条件: queue.size() >= 100 || 定时 50ms
├── 写入: MyBatis batch insert
├── 失败: 重试 3 次 (指数退避)
└── 降级: 写本地文件 (JSON 行格式, 按日期分文件)
```

---

## 六、监控指标体系

### 6.1 核心指标

| 指标名 | 类型 | 标签 | 说明 |
|--------|------|------|------|
| `data_check_total` | Counter | `result={success\|fail}`, `method` | 核对总数 |
| `data_check_duration_seconds` | Timer | `rule_type`, `method` | 核对耗时分布 |
| `expression_cache_total` | Counter | `result={hit\|miss}` | 缓存命中统计 |
| `expression_compile_duration_seconds` | Timer | `rule_type` | 编译耗时 |
| `kafka_consumer_lag` | Gauge | `topic`, `partition` | 消费延迟 |
| `fail_write_queue_size` | Gauge | - | 失败队列积压 |

### 6.2 Grafana 看板

```
┌─────────────────────────────────────────────────────────────┐
│                    数据核对监控看板                           │
├──────────────────────┬──────────────────────────────────────┤
│  实时成功率 (Stat)    │  核对量趋势 (Graph)                   │
│  99.7%               │  ┌──────┐                            │
│                      │  │ █████│ 成功                        │
│                      │  │ ▓▓▓  │ 失败                        │
├──────────────────────┼──────────────────────────────────────┤
│  缓存命中率 (Gauge)   │  耗时分布 (Histogram)                 │
│  99.2%               │  P50: 0.5ms  P99: 3ms               │
├──────────────────────┼──────────────────────────────────────┤
│  消费延迟 (Gauge)     │  失败待确认 (Table)                   │
│  1,200 条            │  规则A: 15条  规则B: 8条             │
└──────────────────────┴──────────────────────────────────────┘
```

---

## 七、部署架构

```
┌─────────────────────────────────────────────────────────────┐
│                      生产部署                                │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Kafka Cluster (3节点)                                       │
│  ├── Topic: data-check-msg (12分区)                         │
│  └── Topic: rule-config-changes (3分区)                     │
│                                                             │
│  核对服务 (3实例)                                            │
│  ├── 每个实例消费 4 个分区                                    │
│  ├── 线程池: 16核 / 实例                                     │
│  └── 堆内存: 4G (G1 GC)                                     │
│                                                             │
│  Redis (哨兵模式)                                            │
│  └── 规则配置缓存                                            │
│                                                             │
│  MySQL (主从)                                                │
│  ├── 主库: 写入失败数据                                      │
│  └── 从库: 后台查询 / 人工确认                               │
│                                                             │
│  Prometheus + Grafana                                        │
│  └── 指标采集 + 告警 (成功率 < 99% 告警)                     │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```
