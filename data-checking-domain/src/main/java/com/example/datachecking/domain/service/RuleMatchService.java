package com.example.datachecking.domain.service;

import com.example.datachecking.domain.model.CheckRule;
import java.util.List;

/**
 * 规则匹配领域服务接口
 * <p>
 * 职责: 根据方法名匹配适用的核对规则，支持精确匹配、通配匹配和正则匹配三种模式。
 * <p>
 * DDD 角色: 领域服务接口，定义在领域层，实现在基础设施层（RuleIndexService）。
 * 遵循依赖倒置原则，领域层不依赖具体实现。
 */
public interface RuleMatchService {

    /**
     * 根据方法名匹配所有适用的规则
     *
     * @param methodName 业务方法名，如 "orderService.createOrder"
     * @return 匹配的规则列表，按优先级降序排列
     */
    List<CheckRule> matchRules(String methodName);

    /**
     * 重建规则索引
     * <p>
     * 触发时机: 规则创建/更新/删除后，或启动时从数据库加载全量规则。
     * 作用: 将数据库中的规则构建为内存索引（exactMatch/wildcardMatch/regexMatch），
     *       提升后续匹配性能至 O(1)。
     */
    void rebuildIndex();
}
