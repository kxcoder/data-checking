package com.example.datachecking.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.datachecking.domain.model.CheckRecord;
import com.example.datachecking.domain.model.CheckResult;
import com.example.datachecking.domain.model.ConfirmStatus;
import com.example.datachecking.domain.repository.CheckRecordRepository;
import com.example.datachecking.infrastructure.persistence.entity.CheckRecordEntity;
import com.example.datachecking.infrastructure.persistence.mapper.CheckRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CheckRecordRepositoryImpl implements CheckRecordRepository {

    private final CheckRecordMapper checkRecordMapper;

    @Override
    public void save(CheckRecord record) {
        CheckRecordEntity entity = toEntity(record);
        if (entity.getId() == null) {
            checkRecordMapper.insert(entity);
        } else {
            checkRecordMapper.updateById(entity);
        }
    }

    @Override
    public Optional<CheckRecord> findById(Long id) {
        CheckRecordEntity entity = checkRecordMapper.selectById(id);
        return Optional.ofNullable(entity).map(this::toDomain);
    }

    @Override
    public List<CheckRecord> findByConfirmStatus(ConfirmStatus status, int page, int size) {
        Page<CheckRecordEntity> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<CheckRecordEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CheckRecordEntity::getConfirmStatus, status.getCode())
               .orderByDesc(CheckRecordEntity::getCreatedAt);
        return checkRecordMapper.selectPage(pageObj, wrapper).getRecords().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public long countByConfirmStatus(ConfirmStatus status) {
        LambdaQueryWrapper<CheckRecordEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CheckRecordEntity::getConfirmStatus, status.getCode());
        return checkRecordMapper.selectCount(wrapper);
    }

    private CheckRecord toDomain(CheckRecordEntity entity) {
        return CheckRecord.builder()
                .id(entity.getId())
                .traceId(entity.getTraceId())
                .ruleId(entity.getRuleId())
                .methodName(entity.getMethodName())
                .checkResult(CheckResult.fromCode(entity.getCheckResult()))
                .expression(entity.getExpression())
                .inputParams(entity.getInputParams())
                .returnData(entity.getReturnData())
                .failReason(entity.getFailReason())
                .confirmStatus(ConfirmStatus.fromCode(entity.getConfirmStatus()))
                .confirmUser(entity.getConfirmUser())
                .confirmRemark(entity.getConfirmRemark())
                .confirmAt(entity.getConfirmAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private CheckRecordEntity toEntity(CheckRecord record) {
        CheckRecordEntity entity = new CheckRecordEntity();
        entity.setId(record.getId());
        entity.setTraceId(record.getTraceId());
        entity.setRuleId(record.getRuleId());
        entity.setMethodName(record.getMethodName());
        entity.setCheckResult(record.getCheckResult().getCode());
        entity.setExpression(record.getExpression());
        entity.setInputParams(record.getInputParams());
        entity.setReturnData(record.getReturnData());
        entity.setFailReason(record.getFailReason());
        entity.setConfirmStatus(record.getConfirmStatus().getCode());
        entity.setConfirmUser(record.getConfirmUser());
        entity.setConfirmRemark(record.getConfirmRemark());
        entity.setConfirmAt(record.getConfirmAt());
        entity.setCreatedAt(record.getCreatedAt());
        return entity;
    }
}
