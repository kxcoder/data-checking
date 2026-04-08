package com.example.datachecking.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 核对结果记录聚合根
 * <p>
 * 业务含义: 记录单次数据核对的结果，包含失败原因、人工确认状态等信息。
 * 当核对失败时，系统自动创建该记录，等待后台人工确认。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckRecord {
    private Long id;
    private String traceId;
    private Long ruleId;
    private String methodName;
    private CheckResult checkResult;
    private String expression;
    private String inputParams;
    private String returnData;
    private String failReason;
    private ConfirmStatus confirmStatus;
    private String confirmUser;
    private String confirmRemark;
    private LocalDateTime confirmAt;
    private LocalDateTime createdAt;

    public void confirm(String user, String remark) {
        validateConfirmInput(user);
        if (confirmStatus == ConfirmStatus.CONFIRMED_NORMAL) {
            throw new IllegalStateException("记录已确认为正常，不能重复确认");
        }
        this.confirmStatus = ConfirmStatus.CONFIRMED_NORMAL;
        this.confirmUser = user;
        this.confirmRemark = remark;
        this.confirmAt = LocalDateTime.now();
    }

    public void markAsAbnormal(String user, String remark) {
        validateConfirmInput(user);
        if (confirmStatus == ConfirmStatus.CONFIRMED_ABNORMAL) {
            throw new IllegalStateException("记录已确认为异常，不能重复标记");
        }
        this.confirmStatus = ConfirmStatus.CONFIRMED_ABNORMAL;
        this.confirmUser = user;
        this.confirmRemark = remark;
        this.confirmAt = LocalDateTime.now();
    }

    private void validateConfirmInput(String user) {
        if (user == null || user.isBlank()) {
            throw new IllegalArgumentException("确认人不能为空");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CheckRecord that = (CheckRecord) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}