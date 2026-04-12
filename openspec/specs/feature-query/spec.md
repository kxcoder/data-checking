# Feature Query - 特征查询能力

## Overview

根据用户 UID 和特征键列表，查询并返回用户特征数据。支持多数据源、转换脚本、三级缓存。

解决：营销决策需要用户特征数据的问题，需要从不同数据源获取并统一返回。

## ADDED Requirements

### Requirement: 特征配置模型

系统 SHALL 定义特征配置模型，包含特征键、数据源类型、转换类型等。

**Priority**: P0 (Critical)

**Rationale**: 特征配置是查询的基础。

#### Scenario: 创建特征配置

Given 需要创建特征配置
When 提供 { featureKey, supplierType, supplierKey, transformType }
Then 配置创建成功
And enabled 默认为 true

---

### Requirement: 特征查询请求

系统 SHALL 接收特征查询请求，包含用户ID和特征键列表。

**Priority**: P0 (Critical)

**Rationale**: 特征查询的入口。

#### Scenario: 创建特征查询请求

Given 需要查询用户特征
When 提供 uid 和 featureKeys
Then 请求创建成功
AndDubbo RPC 返回 FeatureQueryResponse

---

### Requirement: 多数据源获取

系统 SHALL 从不同数据源获取特征原始数据，支持 LOCAL、REDIS、DB、HTTP。

**Priority**: P0 (Critical)

**Rationale**: 特征数据分布在不同数据源。

#### Scenario: Redis 数据源获取

Given supplierType=REDIS, supplierKey="user:profile"
When 查询特征
Then 从 Redis 获取原始数据

#### Scenario: 数据库数据源获取

Given supplierType=DB, supplierKey="t_user_profile"
When 查询特征
And 从 MySQL 获取原始数据

#### Scenario: HTTP 数据源获取

Given supplierType=HTTP, supplierKey="/api/user/{uid}/profile"
When 查询特征
Then 调用 HTTP 接口获取数据

---

### Requirement: 特征数据转换

系统 SHALL 对原始数据进行转换，支持 NONE、SPEL、GROOVY 转换类型。

**Priority**: P0 (Critical)

**Rationale**: 不同数据源的数据格式可能不同，需要统一转换。

#### Scenario: JSONPath 提取

Given extractPath="$.data.name"
When 提取数据
Then 返回 JsonPath 解析结果

#### Scenario: SpEL 转换

Given transformType=SPEL, transformScript="#value * 100"
When 转换数据
And 原始值 0.5
Then 返回 50

#### Scenario: Groovy 转换

Given transformType=GROOVY, transformScript="value.collect { it.toUpperCase() }"
When 转换数据 ["a", "b"]
Then 返回 ["A", "B"]

---

### Requirement: 特征缓存

系统 SHALL 对查询结果进行三级缓存：L1 本地、L2 Redis、L3 数据源。

**Priority**: P1 (Important)

**Rationale**: 提升查询性能，减少数据源压力。

#### Scenario: L1 本地缓存

Given 首次查询
When 写入 L1 缓存
Then 下次查询从 L1 返回

#### Scenario: L2 Redis 缓存

Given L1 未命中
When 查询 L2 缓存
Then 返回并回填 L1

#### Scenario: 缓存过期

Given 缓存超过 TTL
When 再次查询
And 从数据源获取
And 更新缓存

---

### Requirement: 并行数据获取

系统 SHALL 并行获取不同数据源的特征数据。

**Priority**: P1 (Important)

**Rationale**: 提升查询性能。

#### Scenario: 并行获取多数据源

Given 多个数据源
When 查询特征
Then 并行获取各数据源数据
And 合并结果

---

## MODIFIED Requirements

### Requirement: 特征查询响应

系统 SHALL 返回特征查询响应，包含特征值、来源、时间戳。

**Priority**: P0 (Critical)

**Rationale**: 响应需要返回给调用方。

#### Scenario: 响应格式

Given 特征查询结果
When 转换为 FeatureQueryResponse
Then 包含 features, sources, timestamps