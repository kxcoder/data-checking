package com.example.datachecking.domain.repository;

import com.example.datachecking.domain.model.CheckRecord;
import com.example.datachecking.domain.model.ConfirmStatus;
import java.util.List;
import java.util.Optional;

public interface CheckRecordRepository {
    void save(CheckRecord record);
    Optional<CheckRecord> findById(Long id);
    List<CheckRecord> findByConfirmStatus(ConfirmStatus status, int page, int size);
    long countByConfirmStatus(ConfirmStatus status);
}
