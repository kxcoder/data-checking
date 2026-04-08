package com.example.datachecking.adapter.web;

import com.example.datachecking.application.command.ConfirmRecordCommand;
import com.example.datachecking.application.command.CreateRuleCommand;
import com.example.datachecking.application.command.UpdateRuleCommand;
import com.example.datachecking.application.dto.CheckMetricsDTO;
import com.example.datachecking.application.dto.CheckRecordDTO;
import com.example.datachecking.application.dto.CheckRuleDTO;
import com.example.datachecking.application.dto.PageResult;
import com.example.datachecking.application.dto.RuleVersionResult;
import com.example.datachecking.application.service.DataCheckApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/data-check")
@RequiredArgsConstructor
public class DataCheckController {

    private final DataCheckApplicationService applicationService;

    @PostMapping("/rules")
    public ResponseEntity<CheckRuleDTO> createRule(@Validated @RequestBody CreateRuleCommand command) {
        return ResponseEntity.ok(applicationService.createRule(command));
    }

    @PutMapping("/rules/{id}")
    public ResponseEntity<CheckRuleDTO> updateRule(@PathVariable Long id, @RequestBody UpdateRuleCommand command) {
        return ResponseEntity.ok(applicationService.updateRule(command));
    }

    @DeleteMapping("/rules/{id}")
    public ResponseEntity<Void> deleteRule(@PathVariable Long id) {
        applicationService.deleteRule(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/rules")
    public ResponseEntity<List<CheckRuleDTO>> listRules() {
        return ResponseEntity.ok(applicationService.listAllRules());
    }

    @GetMapping("/rules/{id}")
    public ResponseEntity<CheckRuleDTO> getRule(@PathVariable Long id) {
        return ResponseEntity.ok(applicationService.getRule(id));
    }

    @GetMapping("/records/pending")
    public ResponseEntity<PageResult<CheckRecordDTO>> listPendingRecords(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(applicationService.listPendingRecords(page, size));
    }

    @PostMapping("/records/confirm")
    public ResponseEntity<Void> confirmRecord(@Validated @RequestBody ConfirmRecordCommand command) {
        applicationService.confirmRecord(command);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/metrics")
    public ResponseEntity<CheckMetricsDTO> getMetrics() {
        return ResponseEntity.ok(applicationService.getMetrics());
    }

    @GetMapping("/rules/{id}/versions")
    public ResponseEntity<RuleVersionResult> getVersions(@PathVariable Long id) {
        return ResponseEntity.ok(applicationService.getVersions(id));
    }

    @PostMapping("/rules/{id}/publish/{versionId}")
    public ResponseEntity<Void> publishVersion(@PathVariable Long id, @PathVariable Long versionId) {
        applicationService.publishVersion(id, versionId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/rules/{id}/rollback/{versionId}")
    public ResponseEntity<Void> rollbackVersion(@PathVariable Long id, @PathVariable Long versionId) {
        applicationService.rollbackVersion(id, versionId);
        return ResponseEntity.ok().build();
    }
}
