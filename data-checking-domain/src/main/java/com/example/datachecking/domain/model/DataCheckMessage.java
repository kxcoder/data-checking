package com.example.datachecking.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

/**
 * Kafka 消息值对象
 * <p>
 * 数据来源: 业务系统推送的方法调用信息，包含方法名、入参、返回值等。
 * 每条消息代表一次业务方法调用的完整记录，用于后续的数据核对。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataCheckMessage {
    /** 链路追踪ID，关联原始业务请求 */
    private String traceId;
    /** 业务方法全限定名，如 "orderService.createOrder" */
    private String methodName;
    /** 方法调用时间戳（毫秒） */
    private Long timestamp;
    /** 方法入参，key 为参数名，value 为参数值 */
    private Map<String, Object> inputParams;
    /** 方法返回值，可能是对象、Map 或基本类型 */
    private Object returnData;
    /** 上下文数据，如 appId、env 等扩展字段 */
    private Map<String, Object> contextData;
}
