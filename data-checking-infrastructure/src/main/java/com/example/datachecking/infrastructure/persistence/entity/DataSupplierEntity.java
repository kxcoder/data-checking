package com.example.datachecking.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_data_supplier")
public class DataSupplierEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String supplierKey;
    private String supplierName;
    private String supplierType;
    private String config;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}