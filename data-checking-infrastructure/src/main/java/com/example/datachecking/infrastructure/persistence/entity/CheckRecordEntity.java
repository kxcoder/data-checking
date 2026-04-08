package com.example.datachecking.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("t_check_record")
public class CheckRecordEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String traceId;
    private Long ruleId;
    private String methodName;
    private String checkResult;
    private String expression;
    private String inputParams;
    private String returnData;
    private String failReason;
    private Integer confirmStatus;
    private String confirmUser;
    private String confirmRemark;
    private LocalDateTime confirmAt;
    private LocalDateTime createdAt;
}
