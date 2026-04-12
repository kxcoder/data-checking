# Data Checking - 数据核对能力

## Overview

通过 Kafka 消费业务系统消息，根据消息中的方法名匹配核对规则，执行 SpEL/Groovy 表达式进行实时数据核验，失败数据持久化到 MySQL。

解决：业务系统数据异常无法实时发现的问题，确保数据一致性。

## ADDED Requirements

### Requirement: 核对规则模型定义

系统 SHALL 定义核对规则模型，包含规则ID、规则名称、方法匹配模式、规则类型、核验表达式、优先级和启用状态。

**Priority**: P0 (Critical)

**Rationale**: 核对规则是系统的核心实体，是数据核验的基础。

#### Scenario: 创建有效核对规则

Given 需要创建新核对规则
When 提供规则信息 { ruleName, methodPattern, matchType, ruleType, expression, priority }
Then 规则创建成功
And id 自动生成
And enabled 默认为 true
And matchType in (EXACT, WILDCARD, REGEX)
And ruleType in (SPEL, GROOVY)

---

### Requirement: 方法名规则匹配

系统 SHALL 根据消息中的方法名匹配对应的核对规则，支持精确匹配、通配匹配和正则匹配。

**Priority**: P0 (Critical)

**Rationale**: 规则匹配是核验的入口，必须准确快速。

#### Scenario: 精确匹配方法名

Given 规则 methodPattern="orderService.createOrder", matchType=EXACT
When 消息 methodName="orderService.createOrder"
Then 规则匹配成功

#### Scenario: 通配匹配方法名

Given 规则 methodPattern="orderService.*", matchType=WILDCARD
When 消息 methodName="orderService.createOrder"
Then 规则匹配成功

#### Scenario: 正则匹配方法名

Given 规则 methodPattern="order.*\\..*", matchType=REGEX
When 消息 methodName="orderService.createOrder"
Then 规则匹配成功

#### Scenario: 多规则优先级排序

Given 多条匹配的规则
When 规则匹配时
Then 按 priority 降序排列
And 优先级高的规则先执行

---

### Requirement: SpEL 表达式执行

系统 SHALL 支持通过 SpEL 表达式进行规则核验，表达式预编译后缓存复用。

**Priority**: P0 (Critical)

**Rationale**: SpEL 适合简单条件判断，预编译可提升性能。

#### Scenario: SpEL 条件判断

Given 规则 ruleType=SPEL, expression="#amount > 0 && #status == 'SUCCESS'"
When 执行核验，context={ amount: 100, status: "SUCCESS" }
Then 核验通过

#### Scenario: SpEL 预编译缓存

Given 首次执行 SpEL 表达式
When 编译并执行 expression
Then 编译结果缓存
And 下次执行直接使用缓存

---

### Requirement: Groovy 脚本执行

系统 SHALL 支持通过 Groovy 脚本进行规则核验，脚本预编译后缓存复用。

**Priority**: P1 (Important)

**Rationale**: Groovy 适合复杂业务逻辑，如循环、集合操作。

#### Scenario: Groovy 复杂逻辑

Given 规则 ruleType=GROOVY, expression="items.findAll { it.price > 0 }.sum()"
When 执行核验，context={ items: [{price: 10}, {price: 20}] }
Then 核验通过，返回 30

---

### Requirement: 规则变更广播

系统 SHALL 支持规则变更通过 Kafka topic 广播，各实例监听并更新缓存。

**Priority**: P1 (Important)

**Rationale**: 实现规则热更新，无需重启服务。

#### Scenario: 规则更新广播

Given 规则已更新
When 发布 RuleChangeEvent
Then 所有消费实例收到事件
And 清除旧缓存，预编译新规则

---

### Requirement: 失败数据持久化

系统 SHALL 将核验失败的数据异步批量写入 MySQL。

**Priority**: P0 (Critical)

**Rationale**: 失败数据需持久化供人工确认。

#### Scenario: 失败数据异步写入

Given 核验失败
When 写入失败队列
Then 异步批量写入 MySQL
And 成功则清空队列
And 失败则降级写本地文件

---

## MODIFIED Requirements

### Requirement: 规则执行引擎

系统 SHALL 执行核对规则核验，返回 DecisionResult。

**Priority**: P0 (Critical)

**Rationale**: 核心执行逻辑。

#### Scenario: 执行规则核验

Given 规则和消息上下文
When 执行 CheckEngine.execute(rule, message)
Then 返回 DecisionResult
And 包含 passed, ruleId, failReason, traceId