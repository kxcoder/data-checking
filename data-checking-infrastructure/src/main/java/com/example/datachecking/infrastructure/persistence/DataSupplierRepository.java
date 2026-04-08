package com.example.datachecking.infrastructure.persistence;

import com.example.datachecking.infrastructure.persistence.entity.DataSupplierEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DataSupplierRepository extends BaseMapper<DataSupplierEntity> {
}