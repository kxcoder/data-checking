# Design: 营销决策能力技术设计

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│  adapter (外层)  │  Dubbo RPC  │  MarketingDecisionServiceImpl   │
│       ↓                                                               │
│  api (端口层)     │  接口定义    │  MarketingDecisionService       │
│       ↓                                                               │
│  application     │  用例服务    │  MarketingDecisionApplicationService │
│       ↓                                                               │
│  infrastructure  │  技术实现    │  MarketingDecisionEngine        │
│       ↓                                                               │
│  domain (核心)   │  领域模型    │  DecisionResult, MarketingDecision │
└─────────────────────────────────────────────────────────────────────┘
```

### 调用链路

```
Dubbo 客户端
    ↓ MarketingDecisionRequest (api/dto)
MarketingDecisionServiceImpl (adapter) 
    ↓ 转换为 MarketingDecisionCommand (application/dto)
MarketingDecisionApplicationService (application)
    ↓ 调用 MarketingDecisionExecutor 接口
MarketingDecisionEngine.execute() (infrastructure)
    ↓ 返回 DecisionResult (domain)
    ↓ 转换回 MarketingDecisionResponse (api/dto)
```

## Core Components

| 组件 | 职责 | 位置 |
|------|------|------|
| `MarketingDecisionService` | Dubbo RPC 接口定义 | `api` 模块 |
| `MarketingDecisionServiceImpl` | RPC 实现，参数校验，DTO 转换 | `adapter` 模块 |
| `MarketingDecisionApplicationService` | 用例编排，参数校验 | `application` 模块 |
| `MarketingDecisionExecutor` | 决策执行接口 | `domain` 模块 |
| `MarketingDecisionEngine` | 规则引擎实现，SpEL/Groovy 执行 | `infrastructure` 模块 |
| `CompiledExpressionCache` | 表达式预编译缓存 | `infrastructure` 模块 |
| `SceneRuleService` | 场景规则查询接口 | `domain` 模块 |

## Data Model

### MarketingDecision（领域模型）

| 字段 | 类型 | 说明 |
|------|------|------|
| sceneCode | String | 场景码 |
| uid | Long | 用户ID |
| params | Map | 决策参数 |
| context | Map | 上下文数据 |
| result | DecisionResult | 决策结果 |

### DecisionResult（领域模型）

| 字段 | 类型 | 说明 |
|------|------|------|
| success | Boolean | 是否成功 |
| decisionCode | String | 决策码（PASS/REJECT/ERROR） |
| message | String | 决策消息 |
| data | Map | 额外数据 |
| processTimeMs | Long | 处理耗时 |

### CheckRule（扩展）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 规则ID |
| ruleName | String | 规则名称 |
| sceneCode | String | 场景码 |
| methodPattern | String | 方法匹配模式 |
| matchType | MatchType | 匹配类型（EXACT/WILDCARD/REGEX） |
| ruleType | RuleType | 规则类型（SPEL/GROOVY） |
| expression | String | 执行表达式 |
| priority | Integer | 优先级 |
| enabled | Boolean | 是否启用 |

## API Design

### Dubbo 接口

```java
public interface MarketingDecisionService {
    MarketingDecisionResponse decide(MarketingDecisionRequest request);
}
```

### 请求/响应格式

**Request:**
```json
{
  "sceneCode": "new-user-gift",
  "uid": 123456,
  "params": { "amount": 100 },
  "context": {}
}
```

**Response:**
```json
{
  "success": true,
  "decisionCode": "PASS",
  "message": "Eligible for new user gift",
  "data": { "giftId": 1001 },
  "processTimeMs": 15
}
```

## Integration Patterns

### 规则引擎模式

1. **SpEL 引擎** - 适合简单条件判断
   - 预编译：首次编译存入缓存
   - 执行：从缓存获取编译结果，传入变量执行

2. **Groovy 引擎** - 适合复杂业务逻辑
   - 预编译：GroovyShell 编译脚本
   - 执行：通过 Binding 传入变量

### 三级缓存模式

1. **L1** - ConcurrentHashMap（进程内）
2. **L2** - Redis（分布式）
3. **L3** - MySQL（持久化）

### 熔断限流模式

- Sentinel 限流：100 QPS
- Sentinel 熔断：50% 错误率，10s 窗口

## Technology Stack

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.5.7 | 框架 |
| Java | 21 | 运行时 |
| Dubbo | 3.3.5 | RPC |
| SpEL | - | 简单规则 |
| Groovy | - | 复杂规则 |
| Sentinel | - | 限流熔断 |
| Redis | - | 缓存 |

## Security

- 输入参数校验（sceneCode 非空，uid 非空）
- 表达式执行超时控制（30ms）
- 错误信息脱敏

## Deployment

- 环境：支持 dev/test/pre/prod
- 端口：20880（Dubbo）
- 依赖：Zookeeper 注册中心

## 备选方案对比

| 方案 | 优点 | 缺点 |
|------|------|------|
| SpEL + Groovy | 性能好，配置灵活 | 学习成本 |
| Drools | 功能强大 | 重量级 |
| JSON 规则 | 简单 | 功能有限 |