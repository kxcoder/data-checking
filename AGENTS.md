# AGENTS.md — data-checking

## 项目概述

Spring Boot 3.5.7, Java 21, Maven 多模块。实时数据核对服务：消费 Kafka 消息，通过 SpEL/Groovy 规则引擎核验数据，向 Prometheus 上报指标，失败数据持久化到 MySQL。

## DDD 六边形分层架构（严格遵守）

```
┌─────────────────────────────────────────────────────────────────────┐
│  adapter (外层)  │  协议接入  │  Web控制器 / Dubbo服务 / CLI    │
│       ↓                                                               │
│  api (端口层)     │  接口定义  │  仅被 adapter 依赖            │
│       ↓                                                               │
│  application     │  用例服务  │  仅依赖 domain               │
│       ↓                                                               │
│  infrastructure  │  技术实现  │  依赖 domain               │
│       ↓                                                               │
│  domain (核心)    │  领域模型  │  无外部依赖                │
└─────────────────────────────────────────────────────────────────────┘
```

### 各层依赖规则（严格遵守）

| 模块 | 可以依赖 | 禁止依赖 | 说明 |
|------|---------|---------|------|
| `domain` | 无 | 任何模块 | 核心领域，包含模型、枚举、仓储接口 |
| `api` | 无 | 任何模块 | 端口接口定义，仅含服务接口 + DTO |
| `application` | `domain` | `api`、`infrastructure` | 用例服务，包含 Command/Result DTO |
| `infrastructure` | `domain` | `api`、`adapter`、`application` | 技术实现：MyBatis、Kafka、SpEL、Redis |
| `adapter` | `api` + `application` | `domain`、`infrastructure` | 协议接入：Dubbo、Web，DTO 转换 |

### DTO 分层规范

- **api/dto/** - 对外接口使用的 Request/Response（由 adapter 转换为 application/dto）
- **application/dto/** - 应用层内部使用的 Command/Result（领域无关）
- Adapter 负责在 API DTO 和 Application DTO 之间进行转换

### 领域服务接口规范

Application 层需要使用基础设施时（如缓存、转换引擎），按以下步骤操作：

1. 在 `domain/service/` 定义接口（如 `CacheService`、`TransformEngine`）
2. 在 `infrastructure/` 实现接口（如 `FeatureLocalCache`、`SpelTransformEngine`）
3. Application 层通过接口依赖注入（IoC），不直接依赖 infrastructure 实现

### 分层检查清单

在提交代码前，自查以下事项：

- [ ] domain 模块是否没有依赖其他任何模块？
- [ ] api 模块是否没有依赖其他任何模块？
- [ ] application 模块是否只依赖 domain？
- [ ] infrastructure 模块是否只依赖 domain？（不依赖 application）
- [ ] adapter 模块是否没有直接依赖 domain 和 infrastructure？
- [ ] Application 层需要基础设施时，是否在 domain 层定义了接口？
- [ ] API 层的 DTO 是否只在 adapter 层使用，没有传入 application 层？
- [ ] Adapter 是否负责将 API DTO 转换为 Application DTO？

### 分层验证命令

```bash
# 验证所有模块依赖关系
mvn dependency:tree -pl data-checking-domain | grep -v "^\[" 
mvn dependency:tree -pl data-checking-api | grep -v "^\[" 
mvn dependency:tree -pl data-checking-application | grep -v "^\[" 
mvn dependency:tree -pl data-checking-infrastructure | grep -v "^\[" 
mvn dependency:tree -pl data-checking-adapter | grep -v "^\[" 
```

### 模块结构

| 模块 | 职责 | 依赖 |
|------|------|------|
| `data-checking-domain` | 核心模型、仓储接口、领域服务接口 | Lombok only |
| `data-checking-api` | Dubbo RPC 接口定义、跨服务 DTO | Lombok only |
| `data-checking-application` | 用例服务、Command/Result DTO | domain |
| `data-checking-infrastructure` | Kafka 消费者、MyBatis-Plus Mapper、SpEL/Groovy 引擎、缓存 | domain |
| `data-checking-adapter` | REST 控制器、Dubbo RPC 实现 | api + application |
| `data-checking-start` | Spring Boot 入口，聚合所有模块 | 以上全部 |

## 常用命令

```bash
# 构建所有模块
mvn clean install

# 运行应用（从仓库根目录执行）
mvn spring-boot:run -pl data-checking-start

# 运行应用，指定环境 profile
mvn spring-boot:run -pl data-checking-start -Dspring-boot.run.profiles=dev

# 打包可执行 jar
mvn package -pl data-checking-start

# 打包时指定环境
mvn package -pl data-checking-start -P dev

# 运行单测
mvn test -pl data-checking-infrastructure -Dtest=MarketingDecisionEngineTest
```

## 多环境配置

环境配置文件位于 `data-checking-start/src/main/resources/`：

| 文件 | 环境 | 说明 |
|------|------|------|
| `application.yml` | 公共配置 | 所有环境共享的默认配置 |
| `application-dev.yml` | 开发环境 | localhost 连接，DEBUG 日志 |
| `application-test.yml` | 测试环境 | 测试服务器地址 |
| `application-pre.yml` | 预发布环境 | 3 节点集群，SSL 连接 |
| `application-prod.yml` | 生产环境 | 生产���群配置，WARN 日志 |

**启动时选择环境**：通过 `--spring.profiles.active=xxx` 或 `SPRING_PROFILES_ACTIVE=xxx` 指定。

**敏感配置**（如数据库密码）：通过环境变量或命令行参数覆盖，例如：
```bash
java -jar data-checking-start.jar --spring.datasource.password=xxx
```

## 入口

`data-checking-start/src/main/java/com/example/datachecking/DataCheckingApplication.java`

基础包：`com.example.datachecking`

## 核心技术栈

- **Kafka**：批量消费、手动 ack、规则变更广播 topic
- **规则引擎**：SpEL（简单条件）+ Groovy（复杂逻辑），均预编译并缓存
- **RPC**：Dubbo 3.3.5 + Zookeeper 注册中心
- **ORM**：MyBatis-Plus 3.5.5 + MySQL 8
- **缓存**：三级 — ConcurrentHashMap (L1) → Redis (L2) → MySQL (L3)
- **限流熔断**：Alibaba Sentinel（100 QPS 限流 + 50% 熔断）
- **指标**：Micrometer + Prometheus
- **JSON**：FastJSON2
- **Lombok**：1.18.34，每个模块都配置了 annotation processor

## 目录约定

### domain 模块（核心层）
```
domain/src/main/java/com/example/datachecking/domain/
├── model/          # 领域模型（CheckRule, DecisionResult, CheckRecord 等）
├── enum/           # 枚举值对象（RuleType, MatchType, ConfirmStatus）
├── event/          # 领域事件（RuleChangeEvent）
├── exception/      # 领域异常
├── repository/     # 仓储接口定义
└── service/       # 领域服务接口（CheckEngine, SceneRuleService, MarketingDecisionExecutor）
```

### api 模块（端口层）
```
api/src/main/java/com/example/datachecking/api/
├── dto/            # 跨服务 DTO（对外接口使用的 Request/Response）
└── *.java         # Dubbo/REST 接口定义
```

### application 模块（应用层）
```
application/src/main/java/com/example/datachecking/application/
├── dto/            # 应用层 DTO（MarketingDecisionCommand, MarketingDecisionResult）
├── command/        # CQRS 命令对象
├── service/        # 应用服务（MarketingDecisionApplicationService）
└── query/         # 查询对象（如需要）
```

### infrastructure 模块（基础设施层）
```
infrastructure/src/main/java/com/example/datachecking/infrastructure/
├── engine/         # 规则引擎实现（MarketingDecisionEngine, SpelCheckEngine, GroovyCheckEngine）
├── supplier/      # 数据补充实现（DataSupplierServiceImpl）
├── cache/          # 缓存服务（CompiledExpressionCache, SceneRuleIndexService）
├── config/         # 技术配置（SentinelRuleConfig, RestTemplateConfig, KafkaConfig）
├── kafka/          # Kafka 消费者、生产者
└── persistence/   # MyBatis Mapper、Entity、Repository 实现
```

### adapter 模块（适配器层）
```
adapter/src/main/java/com/example/datachecking/adapter/
├── config/           # 限流熔断处理（SentinelBlockHandler）
├── dubbo/            # Dubbo RPC 服务实现
└── web/              # REST 控制器
```

## Dubbo RPC 服务（营销决策）

服务接口定义在 `data-checking-api` 模块，实现位于 `data-checking-adapter` 模块：

| 接口 | 方法 | 说明 |
|------|------|------|
| `MarketingDecisionService` | `decide(MarketingDecisionRequest)` | 营销决策（限流+熔断保护） |
| `CheckRuleQueryService` | `listAllRules()` | 查询所有启用的核对规则 |
| `CheckRuleQueryService` | `getRuleById(Long id)` | 按 ID 查询单条规则 |

- **版本**：`1.0.0`，分组：`data-checking`
- **协议**：Dubbo，端口 `20880`
- **注册中心**：Zookeeper（地址因环境而异）

### 营销决策接口调用链路

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

## Sentinel 限流熔断配置

- **限流规则**：100 QPS，队列超时 500ms
- **熔断规则**：50% 错误率，10s 窗口，最小请求数 5

## 配置文件

`data-checking-start/src/main/resources/application.yml` 是配置入口，完整配置项示例：

```yaml
server:
  port: 8080

spring:
  application:
    name: data-checking
  datasource:
    url: jdbc:mysql://...
    username: ...
    password: ...
  kafka:
    bootstrap-servers: ...
  redis:
    host: ...
    port: 6379
    password: ""

dubbo:
  application:
    name: data-checking-provider
  protocol:
    name: dubbo
    port: 20880
  registry:
    address: zookeeper://...
  provider:
    timeout: 3000
```

## 重要注意事项

- **分层严格**：`application` 禁止依赖 `api`，`infrastructure` 禁止依赖 `api`，违反分层会导致编译失败
- **多环境**：必须通过 `--spring.profiles.active` 指定运行环境（dev/test/pre/prod），否则使用默认配置
- **敏感信息**：数据库密码、Redis 密码等敏感配置通过环境变量或命令行参数覆盖，**不要**提交到代码仓库
- **零测试** — 任何模块下都没有 `src/test/` 目录（除 infrastructure 有单测）
- **无 `.gitignore`** — IDE 文件（`.classpath`、`.project`、`.idea/`、`.settings/`）被纳入版本控制
- **无 CI/CD** — 无 `.github/`、无 pre-commit 配置、无 Docker 文件
- **设计文档**：根目录下的 `data-check-design.md` 是权威设计规格书（中文，525 行）

## AI 强制执行约束

### 修改代码前必须验证依赖

在修改任何模块的 pom.xml 或添加新的 import 语句前，**必须**执行以下验证：

```bash
# 1. 检查即将修改的模块是否可以依赖目标模块
# domain 模块：不允许任何依赖
# api 模块：不允许任何依赖  
# application 模块：仅允许依赖 domain
# infrastructure 模块：仅允许依赖 domain
# adapter 模块：仅允许依赖 api + application

# 2. 使用 Maven 验证依赖树
mvn dependency:tree -pl <模块名> | grep <可疑依赖>

# 3. 编译验证（最直接的方式）
mvn compile -pl <模块名>
```

### 依赖违规自动检测脚本

在提交代码前，运行以下脚本自动检测依赖违规：

```bash
#!/bin/bash
# check-dependencies.sh

echo "=== 检查 domain 模块 ==="
mvn dependency:tree -pl data-checking-domain 2>/dev/null | grep -v "^\[" | grep "com.example" | grep -v "data-checking-domain" && echo "❌ domain 依赖了其他内部模块" || echo "✅ domain 无违规依赖"

echo "=== 检查 api 模块 ==="
mvn dependency:tree -pl data-checking-api 2>/dev/null | grep -v "^\[" | grep "com.example" | grep -v "data-checking-api" && echo "❌ api 依赖了其他内部模块" || echo "✅ api 无违规依赖"

echo "=== 检查 application 模块 ==="
mvn dependency:tree -pl data-checking-application 2>/dev/null | grep -v "^\[" | grep "com.example" | grep -v "data-checking-application" | grep "data-checking-domain" || echo "❌ application 依赖了非 domain 模块"
mvn dependency:tree -pl data-checking-application 2>/dev/null | grep -v "^\[" | grep "data-checking-infrastructure\|data-checking-adapter" && echo "❌ application 依赖了 infrastructure 或 adapter"

echo "=== 检查 infrastructure 模块 ==="
mvn dependency:tree -pl data-checking-infrastructure 2>/dev/null | grep -v "^\[" | grep "data-checking-api\|data-checking-adapter\|data-checking-application" && echo "❌ infrastructure 依赖了 api/adapter/application"

echo "=== 检查 adapter 模块 ==="
mvn dependency:tree -pl data-checking-adapter 2>/dev/null | grep -v "^\[" | grep "data-checking-domain\|data-checking-infrastructure" && echo "❌ adapter 依赖了 domain 或 infrastructure"
```

### 分层违规处理流程

1. **发现违规**：当 AI 检测到依赖违规时
2. **分析问题**：确定需要的功能应该属于哪一层
3. **重构方案**：
   - 需要 domain 层的功能 → 在 domain/service/ 定义接口，infrastructure 实现
   - 需要 application 层的功能 → 确保仅通过 adapter 调用 application 服务
   - 需要 infrastructure 的功能 → 不能在 adapter 中直接使用，需要通过 application 层间接调用
4. **验证编译**：修改后运行 `mvn compile` 确认编译通过
5. **运行检测脚本**：确认无依赖违规

### 典型违规场景及解决方案

| 场景 | 违规代码 | 解决方案 |
|------|---------|---------|
| Adapter 需要使用缓存 | `import com.example.datachecking.infrastructure.cache.*` | 在 domain/service/ 定义 CacheService 接口，infrastructure 实现，application 注入使用 |
| Adapter 需要使用规则引擎 | `import com.example.datachecking.infrastructure.engine.*` | 通过 application 服务调用，engine 实现 domain 接口 |
| Application 需要外部服务 | 直接注入 infrastructure Bean | 定义 domain 接口，infrastructure 实现，application 注入接口 |
| Infrastructure 需要 Application 层的 DTO | `import com.example.datachecking.application.dto.*` | 使用 domain 模型或创建独立的 domain DTO |