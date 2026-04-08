package com.example.datachecking.domain.repository;

import com.example.datachecking.domain.model.CheckRule;
import java.util.List;
import java.util.Optional;

/**
 * 规则仓储接口 — 规则聚合根的持久化抽象
 * <p>
 * DDD 角色: 仓储模式接口，定义在领域层，实现在基础设施层。
 * 领域层通过此接口操作持久化，不依赖具体数据库技术。
 */
public interface CheckRuleRepository {

    /** 根据主键查找规则 */
    Optional<CheckRule> findById(Long id);

    /** 查找所有已启用的规则，按优先级降序 */
    List<CheckRule> findAllEnabled();

    /** 保存规则（新增或更新） */
    CheckRule save(CheckRule rule);

    /** 根据主键删除规则 */
    void deleteById(Long id);

    /** 根据方法匹配模式查找规则 */
    List<CheckRule> findByMethodPattern(String methodPattern);
}
