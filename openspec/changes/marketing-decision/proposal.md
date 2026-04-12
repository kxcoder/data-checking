# Proposal: 营销决策能力

## Why

### Background

随着营销业务的发展，营销活动 increasingly 需要根据用户特征和场景进行动态决策。传统的营销决策方式是硬编码规则，无法灵活适配不同的营销场景，也无法快速响应业务变化。

### Problem Statement

当前系统面临以下问题：

1. **规则硬编码** - 营销规则以代码形式硬编码，修改需要重新部署
2. **无法动态配置** - 业务方无法通过界面配置营销规则
3. **场景扩展困难** - 新增营销场景需要开发介入，周期长
4. **决策效率低** - 每次决策都需要执行完整的业务逻辑，性能瓶颈明显

### Alternatives Considered

1. **方案一：硬编码规则** - 直接在代码中写 if-else 逻辑
   - 优点：实现简单
   - 缺点：无法动态修改，扩展困难

2. **方案二：业务拼装** - 使用策略模式 + 工厂模式
   - 优点：可复用部分逻辑
   - 缺点：新增规则仍需修改代码

3. **方案三：规则引擎 + 配置化** （最终选择）
   - 优点：规则可配置、热更新、性能优秀
   - 缺点：初期实现成本较高

## What Changes

### New Resources Added

1. `MarketingDecision` 领域模型
2. `DecisionResult` 决策结果模型
3. `CheckRule` 规则模型（扩展支持营销场景）
4. `SceneRuleService` 场景规则服务接口
5. `MarketingDecisionExecutor` 决策执行器接口
6. `MarketingDecisionEngine` 决策引擎实现（SpEL + Groovy）
7. `MarketingDecisionApplicationService` 应用服务
8. `MarketingDecisionService` Dubbo RPC 接口

### New Capabilities

1. 场景码 + UID 驱动的营销决策
2. SpEL 表达式规则引擎（简单条件）
3. Groovy 脚本引擎（复杂业务逻辑）
4. 规则预编译缓存（L1 内存 + L2 Redis）
5. 限流保护（Sentinel 100 QPS）
6. 熔断保护（50% 错误率）

## Capabilities

### New Capabilities

- `marketing-decision` - 营销决策能力

### Modified Capabilities

- `data-checking` - 扩展支持营销场景规则匹配

## Impact

### In Scope

- 营销决策 Dubbo RPC 接口
- 规则引擎（SpEL + Groovy）
- 三级缓存（内存 + Redis + MySQL）
- 限流熔断（Sentinel）

### Out of Scope

- 规则管理 UI
- 决策可视化
- A/B Testing

## Goals

- 支持 100+ 营销场景配置化
- 单次决策耗时 < 50ms
- 规则热更新，秒级生效

## References

- 现有 `data-checking` 能力规范
- Spring Expression Language 文档
- Apache Groovy 文档