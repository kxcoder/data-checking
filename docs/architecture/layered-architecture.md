# 分层架构设计文档

## 概述

本文档描述了数据核对服务（data-checking）项目的分层架构设计，包括模块划分、依赖规则和设计原则。

## 模块划分

### 1. domain (核心层)

**职责**：领域模型、领域服务接口、仓储接口、领域事件

**依赖规则**：
- 禁止依赖任何其他模块
- 仅允许使用 Lombok 注解处理器

**包含内容**：
- `domain/model/` - 领域模型（CheckRule, DecisionResult, FeatureConfig 等）
- `domain/enum/` - 枚举值对象（RuleType, MatchType, SupplierType 等）
- `domain/event/` - 领域事件
- `domain/exception/` - 领域异常
- `domain/repository/` - 仓储接口定义
- `domain/service/` - 领域服务接口（如 DataSupplierService, CacheService, TransformEngine）

### 2. api (端口层)

**职责**：对外接口定义、跨服务 DTO

**依赖规则**：
- 禁止依赖任何其他模块
- 仅允许使用 Lombok
- Dubbo 接口定义在此模块

**包含内容**：
- `api/dto/` - 跨服务 DTO（FeatureQueryRequest, FeatureQueryResponse）
- `api/*.java` - Dubbo/REST 接口定义

### 3. application (应用层)

**职责**：用例服务、Command/Result DTO、业务流程编排

**依赖规则**：
- 允许依赖 domain
- 禁止依赖 api
- 禁止依赖 infrastructure

**包含内容**：
- `application/dto/` - 应用层 DTO（FeatureQueryCommand, FeatureQueryResult）
- `application/command/` - CQRS 命令对象
- `application/service/` - 应用服务

### 4. infrastructure (基础设施层)

**职责**：技术实现、数据访问、外部服务调用

**依赖规则**：
- 允许依赖 domain
- 禁止依赖 application
- 禁止依赖 api
- 禁止依赖 adapter

**包含内容**：
- `infrastructure/engine/` - 规则引擎实现（SpelTransformEngine, GroovyTransformEngine）
- `infrastructure/supplier/` - 数据供应实现（DataSupplierServiceImpl, RpcSupplierExecutor, RedisSupplierExecutor）
- `infrastructure/cache/` - 缓存实现（FeatureLocalCache, FeatureRedisCache）
- `infrastructure/persistence/` - MyBatis Mapper、Entity、Repository 实现
- `infrastructure/kafka/` - Kafka 消费者、生产者
- `infrastructure/config/` - 技术配置

### 5. adapter (适配器层)

**职责**：协议接入、Dubbo RPC 实现、Web 控制器、DTO 转换

**依赖规则**：
- 允许依赖 application
- 允许依赖 api
- 禁止依赖 domain（通过 application 间接依赖）
- 禁止依赖 infrastructure

**包含内容**：
- `adapter/config/` - 限流熔断处理（SentinelBlockHandler）
- `adapter/dubbo/` - Dubbo RPC 服务实现
- `adapter/web/` - REST 控制器

## 依赖关系图

```
                          data-checking-start
                                 ↑
┌─────────────────────────────────────────────────────────────────────┐
│                        adapter (适配器层)                            │
│              依赖: application + api                                 │
│                              ↑                                       │
│                    application (应用层)                              │
│                        依赖: domain                                  │
│                              ↑                                       │
│                 infrastructure (基础设施层)                          │
│                     依赖: domain                                      │
│                              ↑                                       │
│            api (端口层)  ←→  domain (核心层)                         │
│               依赖: 无              依赖: 无                          │
└─────────────────────────────────────────────────────────────────────┘
```

## 依赖规则表

| 模块 | 可依赖 | 禁止依赖 | 说明 |
|------|--------|---------|------|
| `domain` | 无 | 任何模块 | 核心领域，包含模型、枚举、仓储接口 |
| `api` | 无 | 任何模块 | 端口接口定义，仅含服务接口 + DTO |
| `application` | domain | api、infrastructure | 用例服务，包含 Command/Result DTO |
| `infrastructure` | domain | api、adapter、application | 技术实现：MyBatis、Kafka、SpEL、Redis |
| `adapter` | application + api | domain、infrastructure | 协议接入：Dubbo、Web，DTO 转换 |

## 分层原则

### 1. 依赖方向

依赖必须从外层指向内层：
- adapter → application → domain
- adapter → infrastructure → application → domain
- adapter → api（api 是独立模块，不属于内层或外层）

### 2. 接口定义位置

- **领域服务接口** 定义在 `domain/service/` 包
- **应用服务接口** 不需要接口，直接使用实现类
- **端口接口**（Dubbo/REST）定义在 `api/` 包

### 3. DTO 分层

- **api/dto/** - 对外接口使用的 Request/Response（由 adapter 转换为 application/dto）
- **application/dto/** - 应用层内部使用的 Command/Result（领域无关）

### 4. 基础设施层注入

Application 服务需要的基础设施（CacheService, TransformEngine 等）：
1. 在 domain 层定义接口（如 `CacheService`、`TransformEngine`）
2. 在 infrastructure 层实现接口（如 `FeatureLocalCache`、`SpelTransformEngine`）
3. Application 层通过接口依赖 inversion of control

## 常见问题

### Q: Application 层需要使用缓存怎么办？

A: 
1. 在 `domain/service/` 定义 `CacheService` 接口
2. 在 `infrastructure/cache/` 实现 `CacheService` 接口
3. Application 层注入 `CacheService` 接口

### Q: Application 层需要使用转换引擎怎么办？

A:
1. 在 `domain/service/` 定义 `TransformEngine` 接口
2. 在 `infrastructure/engine/` 实现 `SpelTransformEngine`、`GroovyTransformEngine`
3. Application 层注入 `TransformEngine` 接口（需要两个实现时，注入 List<TransformEngine>）

### Q: 为什么 Application 不能依赖 API？

A: API 层定义的是对外接口的 Request/Response，这些是外部协议相关的 DTO。Application 层的 Command/Result 应该是领域无关的，由 Adapter 负责在 API DTO 和 Application DTO 之间转换。

### Q: 基础设施层如何与应用层交互？

A: 通过依赖倒置（Dependency Inversion）：
1. 在 `domain/service/` 定义接口（如 `MarketingDecisionExecutor`）
2. 在 `infrastructure/` 实现接口（如 `MarketingDecisionEngine`）
3. Application 层注入接口，Spring 自动绑定实现

### Q: 为什么 Infrastructure 不能依赖 Application？

A: Infrastructure 层是技术实现层，应该只依赖 domain 核心层。如果 Infrastructure 依赖 Application，会导致：
- 循环依赖风险
- 违反分层架构的内层优先原则
- 基础设施实现与业务逻辑耦合

## 更新日志

- 2026-04-09: 初始版本，定义五层架构及依赖规则
- 2026-04-09: 修正 infrastructure 依赖规则，仅允许依赖 domain（禁止依赖 application）
- 2026-04-09: 修正 adapter 依赖规则，禁止依赖 infrastructure
