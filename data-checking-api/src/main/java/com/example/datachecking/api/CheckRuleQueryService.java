package com.example.datachecking.api;

import com.example.datachecking.api.dto.CheckRuleDTO;

import java.util.List;

/**
 * 数据核对规则查询 RPC 接口
 * <p>
 * 供外部系统通过 Dubbo 调用，查询当前启用的核对规则。
 */
public interface CheckRuleQueryService {

    /**
     * 查询所有启用的核对规则
     *
     * @return 规则列表
     */
    List<CheckRuleDTO> listAllRules();

    /**
     * 按 ID 查询单条规则详情
     *
     * @param id 规则 ID
     * @return 规则详情，不存在时返回 null
     */
    CheckRuleDTO getRuleById(Long id);
}
