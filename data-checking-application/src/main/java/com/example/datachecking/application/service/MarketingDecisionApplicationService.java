package com.example.datachecking.application.service;

import com.example.datachecking.application.dto.MarketingDecisionCommand;
import com.example.datachecking.application.dto.MarketingDecisionResult;
import com.example.datachecking.domain.service.MarketingDecisionExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MarketingDecisionApplicationService {

    private final MarketingDecisionExecutor marketingDecisionExecutor;

    public MarketingDecisionResult decide(MarketingDecisionCommand command) {
        if (command.getSceneCode() == null || command.getSceneCode().isBlank()) {
            return MarketingDecisionResult.builder()
                    .success(false)
                    .decisionCode("ERROR")
                    .message("sceneCode cannot be blank")
                    .build();
        }

        if (command.getUid() == null) {
            return MarketingDecisionResult.builder()
                    .success(false)
                    .decisionCode("ERROR")
                    .message("uid cannot be null")
                    .build();
        }

        var result = marketingDecisionExecutor.execute(
                command.getSceneCode(),
                command.getUid(),
                command.getParams(),
                command.getContext()
        );

        return MarketingDecisionResult.builder()
                .success(result.getSuccess())
                .decisionCode(result.getDecisionCode())
                .message(result.getMessage())
                .data(result.getData())
                .processTimeMs(result.getProcessTimeMs())
                .build();
    }
}