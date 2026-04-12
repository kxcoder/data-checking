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
And Dubbo RPC 返回 MarketingDecisionResponse

---

### Requirement: 营销决策执行

系统 SHALL 根据场景码和用户特征执行营销决策，返回决策结果。

**Priority**: P0 (Critical)

**Rationale**: 核心决策逻辑。

#### Scenario: 执行营销决策

Given 场景码 "new-user-gift" 和用户 UID
When 执行 MarketingDecisionExecutor
Then 返回 DecisionResult
And 包含 decisionCode, message, data

#### Scenario: 默认决策

Given 无匹配的营销规则
When 执行决策
Then 返回默认决策结果
And decisionCode = "REJECT"
And message = "No rules configured for scene: {sceneCode}"

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
Then 包含 success, decisionCode, message, data

---

### Requirement: SpEL 规则引擎

系统 SHALL 支持 SpEL 表达式进行规则判断，适合简单条件逻辑。

**Priority**: P0 (Critical)

**Rationale**: SpEL 适合简单条件判断，预编译可提升性能。

#### Scenario: SpEL 条件判断

Given 规则 expression="#params['age'] >= 18"
When 执行决策，context={ params: { age: 20 } }
Then 规则通过
And decisionCode = "PASS"

#### Scenario: SpEL 预编译缓存

Given 首次执行 SpEL 表达式
When 编译并执行 expression
Then 编译结果缓存
And 下次执行直接使用缓存

---

### Requirement: Groovy 规则引擎

系统 SHALL 支持 Groovy 脚本进行规则判断，适合复杂业务逻辑。

**Priority**: P1 (Important)

**Rationale**: Groovy 适合复杂业务逻辑，如循环、集合操作。

#### Scenario: Groovy 复杂逻辑

Given 规则 expression="def items = params['items']; items.findAll { it.price > 100 }.sum { it.price }"
When 执行决策
Then 返回符合条件的总价

---

### Requirement: 数据补充服务

系统 SHALL 支持在规则执行过程中动态补充用户特征数据。

**Priority**: P1 (Important)

**Rationale**: 规则可能需要额外的数据源。

#### Scenario: 动态补充数据

Given 规则 expression="#supplier.get('user-tag', params)"
When 执行决策
Then 调用 DataSupplierService 获取数据
And 注入到规则上下文

---

## MODIFIED Requirements

### Requirement: 限流保护

系统 SHALL 对营销决策接口进行限流保护。

**Priority**: P1 (Important)

**Rationale**: 防止突发流量压垮服务。

#### Scenario: QPS 限流

Given 当前 QPS 超过阈值 100
When 新请求到来
Then 拒绝请求
And 返回限流提示 "Rate limit exceeded"

---

### Requirement: 熔断保护

系统 SHALL 对营销决策接口进行熔断保护。

**Priority**: P1 (Important)

**Rationale**: 防止下游故障导致服务不可用。

#### Scenario: 错误率熔断

Given 错误率超过 50%
When 窗口时间 10s
Then 触发熔断
And 返回熔断提示 "Circuit breaker open"

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