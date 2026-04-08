package com.example.datachecking.adapter.dubbo;

import com.example.datachecking.api.CheckRuleQueryService;
import com.example.datachecking.api.dto.CheckRuleDTO;
import com.example.datachecking.application.service.DataCheckApplicationService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.List;

@DubboService(version = "1.0.0", group = "data-checking")
@RequiredArgsConstructor
public class CheckRuleQueryServiceImpl implements CheckRuleQueryService {

    private final DataCheckApplicationService applicationService;

    @Override
    public List<CheckRuleDTO> listAllRules() {
        return applicationService.listAllRules().stream()
                .map(this::toApiDTO)
                .toList();
    }

    @Override
    public CheckRuleDTO getRuleById(Long id) {
        var appDto = applicationService.getRule(id);
        return toApiDTO(appDto);
    }

    private CheckRuleDTO toApiDTO(
            com.example.datachecking.application.dto.CheckRuleDTO source) {
        if (source == null) {
            return null;
        }
        return CheckRuleDTO.builder()
                .id(source.getId())
                .ruleName(source.getRuleName())
                .methodPattern(source.getMethodPattern())
                .matchType(source.getMatchType())
                .ruleType(source.getRuleType())
                .expression(source.getExpression())
                .priority(source.getPriority())
                .enabled(source.getEnabled())
                .version(source.getVersion())
                .createdAt(source.getCreatedAt())
                .updatedAt(source.getUpdatedAt())
                .build();
    }
}
