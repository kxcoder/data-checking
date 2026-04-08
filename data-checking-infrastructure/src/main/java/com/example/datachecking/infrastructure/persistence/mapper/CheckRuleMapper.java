package com.example.datachecking.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.datachecking.infrastructure.persistence.entity.CheckRuleEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CheckRuleMapper extends BaseMapper<CheckRuleEntity> {
}
