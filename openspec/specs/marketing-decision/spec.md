# Marketing Decision - 营销决策能力

## Overview

根据用户 UID 和场景码，结合特征数据，通过规则引擎执行营销决策，返回决策结果。

解决：营销活动需要根据用户特征和场景动态决策的问题。

## ADDED Requirements

### Requirement: 营销决策请求

系统 SHALL 接收营销决策请求，包含场景码、用户ID和决策参数。

**Priority**: P0 (Critical)

**Rationale**: 营销决策的入口。

#### Scenario: 创建营销决策请求

Given 需要进行营销决策
When 提供 sceneCode, uid, params
Then 请求创建成功
AndDubbo RPC 返回 MarketingDecisionResponse

---

### Requirement: 营销决策执行

系统 SHALL 根据场景码和用户特征执行营销决策，返回决策结果。

**Priority**: P0 (Critical)

**Rationale**: 核心决策逻辑。

#### Scenario: 执行营销决策

Given 场景码 "new-user-gift" 和用户 UID
When 执行 MarketingDecisionExecutor
Then 返回 DecisionResult
And 包含 decision, reason, matchedRules

#### Scenario: 默认决策

Given 无匹配的营销规则
When 执行决策
Then 返回默认决策结果
And reason = "no matching rules"

---

### Requirement: 场景规则匹配

系统 SHALL 根据场景码匹配对应的营销规则。

**Priority**: P0 (Critical)

**Rationale**: 决策前需匹配规则。

#### Scenario: 场景规则精确匹配

Given 场景码 "new-user-gift"
When 匹配规则
Then 返回该场景下所有启用的规则

---

### Requirement: 决策结果处理

系统 SHALL 将决策结果转换为响应格式返回给调用方。

**Priority**: P0 (Critical)

**Rationale**: 决策结果需要返回给 Dubbo 客户端。

#### Scenario: 响应转换

Given DecisionResult
When 转换为 MarketingDecisionResponse
Then 包含 decision, reason, extra

---

## MODIFIED Requirements

### Requirement: 限流保护

系统 SHALL 对营销决策接口进行限流保护。

**Priority**: P1 (Important)

**Rationale**: 防止突发流量压垮服务。

#### Scenario: QPS 限流

Given 当前 QPS 超过阈值
When 新请求到来
Then 拒绝请求
And 返回限流提示

---

### Requirement: 熔断保护

系统 SHALL 对营销决策接口进行熔断保护。

**Priority**: P1 (Important)

**Rationale**: 防止下游故障导致服务不可用。

#### Scenario: 错误率熔断

Given 错误率超过 50% 
When 窗口时间 10s
Then 触发熔断
And 返回熔断提示

---

### Requirement: 超时处理

系统 SHALL 对营销决策接口设置超时时间。

**Priority**: P1 (Important)

**Rationale**: 防止请求无限等待。

#### Scenario: 超时返回默认决策

Given 决策执行超时
When 超时时间 3000ms
Then 返回默认决策
And 记录超时日志