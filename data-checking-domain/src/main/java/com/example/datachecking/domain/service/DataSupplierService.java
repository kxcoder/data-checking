package com.example.datachecking.domain.service;

import com.example.datachecking.domain.model.CheckRule;

import java.util.List;

public interface DataSupplierService {
    Object supply(String supplierKey, Object params);
}