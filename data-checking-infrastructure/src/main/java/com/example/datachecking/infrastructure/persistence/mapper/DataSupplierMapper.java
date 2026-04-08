package com.example.datachecking.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.datachecking.infrastructure.persistence.entity.DataSupplierEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DataSupplierMapper extends BaseMapper<DataSupplierEntity> {
}