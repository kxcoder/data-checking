# Tasks: 营销决策能力实现任务清单

## Milestone 1 - Domain Layer（领域层）

### Definition of Done

- 完成领域模型定义
- 领域服务接口定义完成
- domain 模块编译通过

### Tasks

- [x] 定义 MarketingDecision 领域模型（sceneCode, uid, params, context, result）
- [x] 定义 DecisionResult 领域模型（success, decisionCode, message, data, processTimeMs）
- [x] 定义 MarketingDecisionExecutor 接口（execute 方法）
- [x] 定义 SceneRuleService 接口（matchRules 方法）
- [x] 定义 DataSupplierService 接口（supply 方法）

## Milestone 2 - API Layer（接口层）

### Definition of Done

- Dubbo 接口定义完成
- DTO 定义完成
- api 模块编译通过

### Tasks

- [x] 定义 MarketingDecisionService Dubbo 接口
- [x] 定义 MarketingDecisionRequest API DTO
- [x] 定义 MarketingDecisionResponse API DTO

## Milestone 3 - Application Layer（应用层）

### Definition of Done

- 应用服务实现完成
- Command/Result DTO 定义完成
- application 模块编译通过

### Tasks

- [x] 定义 MarketingDecisionCommand 应用层 DTO
- [x] 定义 MarketingDecisionResult 应用层 DTO
- [x] 实现 MarketingDecisionApplicationService
- [x] 实现参数校验逻辑（sceneCode 非空，uid 非空）

## Milestone 4 - Infrastructure Layer（基础设施层）

### Definition of Done

- 规则引擎实现完成
- 缓存实现完成
- infrastructure 模块编译通过

### Tasks

- [x] 实现 MarketingDecisionEngine（规则引擎实现）
- [x] 实现 SpEL 规则执行（executeSpel 方法）
- [x] 实现 Groovy 规则执行（executeGroovy 方法）
- [x] 实现 CompiledExpressionCache（表达式预编译缓存）
- [x] 实现 CompiledGroovyScript（Groovy 脚本编译）
- [x] 实现 SceneRuleServiceImpl（场景规则查询）
- [x] 实现 DataSupplierServiceImpl（数据补充服务）

## Milestone 5 - Adapter Layer（适配器层）

### Definition of Done

- Dubbo RPC 实现完成
- Sentinel 限流熔断配置完成
- adapter 模块编译通过

### Tasks

- [x] 实现 MarketingDecisionServiceImpl（Dubbo RPC 实现）
- [x] 实现 API DTO 到 Application DTO 转换
- [x] 配置 Sentinel 限流规则（100 QPS）
- [x] 配置 Sentinel 熔断规则（50% 错误率）
- [x] 配置 Dubbo 服务暴露（端口 20880）

## Milestone 6 - Testing & Validation（测试验证）

### Definition of Done

- 单元测试通过
- 集成测试通过
- 性能测试达标

### Tasks

- [x] 编写 MarketingDecisionEngineTest 单元测试
- [x] 测试 SpEL 规则执行
- [x] 测试 Groovy 规则执行
- [x] 测试场景规则匹配
- [x] 测试缓存机制
- [x] 性能测试（单次决策 < 50ms）

## Milestone 7 - Documentation & Deployment（文档部署）

### Definition of Done

- 文档完整
- 部署配置完成

### Tasks

- [x] 编写 OpenSpec 变更文档（proposal.md, design.md, tasks.md）
- [x] 配置多环境配置文件（dev, test, pre, prod）
- [x] 验证 mvn clean install 编译通过
- [x] 验证 mvn spring-boot:run 启动成功