package com.example.datachecking.application.service;

import com.example.datachecking.application.command.ConfirmRecordCommand;
import com.example.datachecking.application.command.CreateRuleCommand;
import com.example.datachecking.application.command.UpdateRuleCommand;
import com.example.datachecking.application.dto.CheckMetricsDTO;
import com.example.datachecking.application.dto.CheckRecordDTO;
import com.example.datachecking.application.dto.CheckRuleDTO;
import com.example.datachecking.application.dto.PageResult;
import com.example.datachecking.application.dto.RuleVersionDTO;
import com.example.datachecking.application.dto.RuleVersionResult;
import com.example.datachecking.domain.event.RuleChangeEvent;
import com.example.datachecking.domain.exception.RecordNotFoundException;
import com.example.datachecking.domain.exception.RuleNotFoundException;
import com.example.datachecking.domain.exception.VersionNotFoundException;
import com.example.datachecking.domain.model.CheckRecord;
import com.example.datachecking.domain.model.CheckRule;
import com.example.datachecking.domain.model.ConfirmAction;
import com.example.datachecking.domain.model.ConfirmStatus;
import com.example.datachecking.domain.model.MatchType;
import com.example.datachecking.domain.model.RuleType;
import com.example.datachecking.domain.model.RuleVersion;
import com.example.datachecking.domain.model.VersionStatus;
import com.example.datachecking.domain.repository.CheckRecordRepository;
import com.example.datachecking.domain.repository.CheckRuleRepository;
import com.example.datachecking.domain.repository.RuleVersionRepository;
import com.example.datachecking.domain.service.RuleMatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Validated
public class DataCheckApplicationService {

    private final CheckRuleRepository ruleRepository;
    private final CheckRecordRepository recordRepository;
    private final RuleMatchService ruleMatchService;
    private final ApplicationEventPublisher eventPublisher;
    private final RuleVersionRepository versionRepository;

    @Transactional
    public CheckRuleDTO createRule(CreateRuleCommand command) {
        CheckRule rule = CheckRule.builder()
                .ruleName(command.getRuleName())
                .methodPattern(command.getMethodPattern())
                .matchType(MatchType.fromCode(command.getMatchType()))
                .ruleType(RuleType.fromCode(command.getRuleType()))
                .expression(command.getExpression())
                .priority(command.getPriority() != null ? command.getPriority() : 0)
                .enabled(command.getEnabled() != null ? command.getEnabled() : true)
                .version(1)
                .build();

        CheckRule saved = ruleRepository.save(rule);
        
        RuleVersion version = RuleVersion.builder()
                .ruleId(saved.getId())
                .ruleName(saved.getRuleName())
                .methodPattern(saved.getMethodPattern())
                .matchType(saved.getMatchType().getCode())
                .ruleType(saved.getRuleType().getCode())
                .expression(saved.getExpression())
                .priority(saved.getPriority())
                .enabled(saved.getEnabled())
                .ruleVersion(1)
                .status(VersionStatus.PUBLISHED.getCode())
                .publishedAt(LocalDateTime.now())
                .createdBy("system")
                .createdAt(LocalDateTime.now())
                .build();
        versionRepository.save(version);
        
        eventPublisher.publishEvent(new RuleChangeEvent(saved.getId(), RuleChangeEvent.ChangeType.CREATE));
        ruleMatchService.rebuildIndex();
        return toRuleDTO(saved);
    }

    @Transactional
    public CheckRuleDTO updateRule(UpdateRuleCommand command) {
        CheckRule existing = ruleRepository.findById(command.getId())
                .orElseThrow(() -> new RuleNotFoundException(command.getId()));

        if (command.getRuleName() != null) existing.setRuleName(command.getRuleName());
        if (command.getMethodPattern() != null) existing.setMethodPattern(command.getMethodPattern());
        if (command.getMatchType() != null) existing.setMatchType(MatchType.fromCode(command.getMatchType()));
        if (command.getRuleType() != null) existing.setRuleType(RuleType.fromCode(command.getRuleType()));
        if (command.getExpression() != null) existing.setExpression(command.getExpression());
        if (command.getPriority() != null) existing.setPriority(command.getPriority());
        if (command.getEnabled() != null) existing.setEnabled(command.getEnabled());
        existing.setVersion(existing.getVersion() + 1);
        existing.setUpdatedAt(LocalDateTime.now());

        CheckRule saved = ruleRepository.save(existing);
        
        RuleVersion version = RuleVersion.builder()
                .ruleId(saved.getId())
                .ruleName(saved.getRuleName())
                .methodPattern(saved.getMethodPattern())
                .matchType(saved.getMatchType().getCode())
                .ruleType(saved.getRuleType().getCode())
                .expression(saved.getExpression())
                .priority(saved.getPriority())
                .enabled(saved.getEnabled())
                .ruleVersion(saved.getVersion())
                .status(VersionStatus.DRAFT.getCode())
                .createdBy("system")
                .createdAt(LocalDateTime.now())
                .build();
        versionRepository.save(version);
        
        eventPublisher.publishEvent(new RuleChangeEvent(saved.getId(), RuleChangeEvent.ChangeType.UPDATE));
        ruleMatchService.rebuildIndex();
        return toRuleDTO(saved);
    }

    @Transactional
    public void deleteRule(Long id) {
        ruleRepository.findById(id).orElseThrow(() -> new RuleNotFoundException(id));
        ruleRepository.deleteById(id);
        versionRepository.findByRuleId(id).forEach(v -> versionRepository.updateStatus(v.getId(), VersionStatus.HISTORY.getCode()));
        eventPublisher.publishEvent(new RuleChangeEvent(id, RuleChangeEvent.ChangeType.DELETE));
        ruleMatchService.rebuildIndex();
    }

    public List<CheckRuleDTO> listAllRules() {
        return ruleRepository.findAllEnabled().stream()
                .map(this::toRuleDTO)
                .toList();
    }

    public CheckRuleDTO getRule(Long id) {
        return ruleRepository.findById(id)
                .map(this::toRuleDTO)
                .orElseThrow(() -> new RuleNotFoundException(id));
    }

    public PageResult<CheckRecordDTO> listPendingRecords(int page, int size) {
        if (page < 1) page = 1;
        if (size < 1) size = 10;
        if (size > 100) size = 100;
        
        long total = recordRepository.countByConfirmStatus(ConfirmStatus.PENDING);
        List<CheckRecordDTO> records = recordRepository.findByConfirmStatus(ConfirmStatus.PENDING, page, size).stream()
                .map(this::toRecordDTO)
                .toList();
        return PageResult.<CheckRecordDTO>builder()
                .records(records)
                .total(total)
                .page(page)
                .size(size)
                .build();
    }

    @Transactional
    public void confirmRecord(ConfirmRecordCommand command) {
        CheckRecord record = recordRepository.findById(command.getId())
                .orElseThrow(() -> new RecordNotFoundException(command.getId()));

        ConfirmAction action = ConfirmAction.fromCode(command.getConfirmType());
        if (action == ConfirmAction.CONFIRM) {
            record.confirm(command.getConfirmUser(), command.getRemark());
        } else {
            record.markAsAbnormal(command.getConfirmUser(), command.getRemark());
        }
        recordRepository.save(record);
    }

    public CheckMetricsDTO getMetrics() {
        long pendingCount = recordRepository.countByConfirmStatus(ConfirmStatus.PENDING);
        return CheckMetricsDTO.builder()
                .pendingConfirmCount(pendingCount)
                .build();
    }

    private CheckRuleDTO toRuleDTO(CheckRule rule) {
        return CheckRuleDTO.builder()
                .id(rule.getId())
                .ruleName(rule.getRuleName())
                .methodPattern(rule.getMethodPattern())
                .matchType(rule.getMatchType() != null ? rule.getMatchType().getCode() : null)
                .ruleType(rule.getRuleType() != null ? rule.getRuleType().getCode() : null)
                .expression(rule.getExpression())
                .priority(rule.getPriority())
                .enabled(rule.getEnabled())
                .version(rule.getVersion())
                .createdAt(rule.getCreatedAt())
                .updatedAt(rule.getUpdatedAt())
                .build();
    }

    private CheckRecordDTO toRecordDTO(CheckRecord record) {
        return CheckRecordDTO.builder()
                .id(record.getId())
                .traceId(record.getTraceId())
                .ruleId(record.getRuleId())
                .methodName(record.getMethodName())
                .checkResult(record.getCheckResult() != null ? record.getCheckResult().getCode() : null)
                .expression(record.getExpression())
                .inputParams(record.getInputParams())
                .returnData(record.getReturnData())
                .failReason(record.getFailReason())
                .confirmStatus(record.getConfirmStatus() != null ? record.getConfirmStatus().getCode() : null)
                .confirmUser(record.getConfirmUser())
                .confirmRemark(record.getConfirmRemark())
                .confirmAt(record.getConfirmAt())
                .createdAt(record.getCreatedAt())
                .build();
    }

    public RuleVersionResult getVersions(Long ruleId) {
        List<RuleVersion> versions = versionRepository.findByRuleId(ruleId);
        RuleVersion current = versionRepository.findByRuleIdAndStatus(ruleId, VersionStatus.PUBLISHED.getCode()).orElse(null);
        return RuleVersionResult.builder()
                .versions(versions.stream().map(this::toVersionDTO).toList())
                .currentVersion(current != null ? toVersionDTO(current) : null)
                .build();
    }

    @Transactional
    public void publishVersion(Long ruleId, Long versionId) {
        RuleVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new VersionNotFoundException(versionId));
        
        if (!version.getRuleId().equals(ruleId)) {
            throw new IllegalArgumentException("版本 " + versionId + " 不属于规则 " + ruleId);
        }

        versionRepository.findByRuleIdAndStatus(ruleId, VersionStatus.PUBLISHED.getCode())
                .ifPresent(v -> versionRepository.updateStatus(v.getId(), VersionStatus.HISTORY.getCode()));
        
        versionRepository.updateStatus(versionId, VersionStatus.PUBLISHED.getCode());
        
        version.setPublishedAt(LocalDateTime.now());
        versionRepository.save(version);

        CheckRule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new RuleNotFoundException(ruleId));
        rule.setRuleName(version.getRuleName());
        rule.setMethodPattern(version.getMethodPattern());
        rule.setMatchType(MatchType.fromCodeOrNull(version.getMatchType()));
        rule.setRuleType(version.getRuleType() != null ? RuleType.fromCode(version.getRuleType()) : null);
        rule.setExpression(version.getExpression());
        rule.setPriority(version.getPriority());
        rule.setEnabled(version.getEnabled());
        rule.setUpdatedAt(LocalDateTime.now());
        ruleRepository.save(rule);

        eventPublisher.publishEvent(new RuleChangeEvent(ruleId, RuleChangeEvent.ChangeType.REFRESH_ALL));
        ruleMatchService.rebuildIndex();
    }

    @Transactional
    public void rollbackVersion(Long ruleId, Long versionId) {
        RuleVersion target = versionRepository.findById(versionId)
                .orElseThrow(() -> new VersionNotFoundException(versionId));
        
        if (!target.getRuleId().equals(ruleId)) {
            throw new IllegalArgumentException("版本 " + versionId + " 不属于规则 " + ruleId);
        }

        versionRepository.findByRuleIdAndStatus(ruleId, VersionStatus.PUBLISHED.getCode())
                .ifPresent(v -> versionRepository.updateStatus(v.getId(), VersionStatus.HISTORY.getCode()));
        
        int newVersionNum = versionRepository.findByRuleId(ruleId).stream()
                .map(RuleVersion::getRuleVersion)
                .max(Integer::compare)
                .orElse(0) + 1;

        RuleVersion newVersion = RuleVersion.builder()
                .ruleId(ruleId)
                .ruleName(target.getRuleName())
                .methodPattern(target.getMethodPattern())
                .matchType(target.getMatchType())
                .ruleType(target.getRuleType())
                .expression(target.getExpression())
                .priority(target.getPriority())
                .enabled(target.getEnabled())
                .ruleVersion(newVersionNum)
                .status(VersionStatus.PUBLISHED.getCode())
                .publishedAt(LocalDateTime.now())
                .createdBy("system")
                .createdAt(LocalDateTime.now())
                .build();
        versionRepository.save(newVersion);

        CheckRule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new RuleNotFoundException(ruleId));
        rule.setRuleName(target.getRuleName());
        rule.setMethodPattern(target.getMethodPattern());
        rule.setMatchType(MatchType.fromCodeOrNull(target.getMatchType()));
        rule.setRuleType(target.getRuleType() != null ? RuleType.fromCode(target.getRuleType()) : null);
        rule.setExpression(target.getExpression());
        rule.setPriority(target.getPriority());
        rule.setEnabled(target.getEnabled());
        rule.setUpdatedAt(LocalDateTime.now());
        ruleRepository.save(rule);

        eventPublisher.publishEvent(new RuleChangeEvent(ruleId, RuleChangeEvent.ChangeType.REFRESH_ALL));
        ruleMatchService.rebuildIndex();
    }

    private RuleVersionDTO toVersionDTO(RuleVersion version) {
        return RuleVersionDTO.builder()
                .id(version.getId())
                .ruleId(version.getRuleId())
                .ruleName(version.getRuleName())
                .methodPattern(version.getMethodPattern())
                .matchType(version.getMatchType())
                .ruleType(version.getRuleType())
                .expression(version.getExpression())
                .priority(version.getPriority())
                .enabled(version.getEnabled())
                .ruleVersion(version.getRuleVersion())
                .status(version.getStatus())
                .publishedAt(version.getPublishedAt())
                .createdBy(version.getCreatedBy())
                .createdAt(version.getCreatedAt())
                .build();
    }
}