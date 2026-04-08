-- =============================================
-- 营销决策服务表结构
-- =============================================

-- 数据补全配置表
CREATE TABLE IF NOT EXISTS `t_data_supplier` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `supplier_key` VARCHAR(64) NOT NULL COMMENT '供应商唯一标识',
    `supplier_name` VARCHAR(100) NOT NULL COMMENT '供应商名称',
    `supplier_type` VARCHAR(20) NOT NULL COMMENT '类型: HTTP/DB',
    `config` JSON NOT NULL COMMENT '配置(JSON)',
    `enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用: 0-禁用 1-启用',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_supplier_key` (`supplier_key`),
    INDEX `idx_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据补全配置表';

-- 场景-规则关联表
CREATE TABLE IF NOT EXISTS `t_scene_rule` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `scene_code` VARCHAR(64) NOT NULL COMMENT '场景编码',
    `rule_id` BIGINT NOT NULL COMMENT '规则ID',
    `rule_priority` INT NOT NULL DEFAULT 0 COMMENT '规则执行优先级',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE INDEX `uk_scene_rule` (`scene_code`, `rule_id`),
    INDEX `idx_scene_code` (`scene_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='场景-规则关联表';

-- 扩展 t_check_rule 表，添加场景相关字段
ALTER TABLE `t_check_rule` 
    ADD COLUMN `scene_code` VARCHAR(64) NULL COMMENT '营销决策场景编码' AFTER `rule_name`,
    ADD COLUMN `output_fields` JSON NULL COMMENT '输出字段配置(JSON)' AFTER `expression`;

CREATE INDEX `idx_scene_code` ON `t_check_rule` (`scene_code`);

-- =============================================
-- 数据核对服务表结构
-- =============================================

-- 核对规则表
CREATE TABLE IF NOT EXISTS `t_check_rule` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `rule_name` VARCHAR(100) NOT NULL COMMENT '规则名称',
    `method_pattern` VARCHAR(200) NOT NULL COMMENT '方法匹配模式',
    `match_type` TINYINT NOT NULL DEFAULT 1 COMMENT '匹配类型: 1-精确匹配 2-通配匹配 3-正则匹配',
    `rule_type` VARCHAR(20) NOT NULL COMMENT '规则类型: SPEL/GROOVY',
    `expression` TEXT NOT NULL COMMENT '核对表达式',
    `priority` INT NOT NULL DEFAULT 0 COMMENT '优先级',
    `enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用: 0-禁用 1-启用',
    `version` INT NOT NULL DEFAULT 1 COMMENT '版本号',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_method_pattern` (`method_pattern`),
    INDEX `idx_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='核对规则表';

-- 核对记录表
CREATE TABLE IF NOT EXISTS `t_check_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `trace_id` VARCHAR(64) NOT NULL COMMENT '链路追踪ID',
    `rule_id` BIGINT NOT NULL COMMENT '规则ID',
    `method_name` VARCHAR(200) NOT NULL COMMENT '方法名',
    `check_result` VARCHAR(20) NOT NULL COMMENT '核对结果: PASS/FAIL',
    `expression` TEXT NOT NULL COMMENT '执行的表达式',
    `input_params` TEXT COMMENT '输入参数(JSON)',
    `return_data` TEXT COMMENT '返回数据(JSON)',
    `fail_reason` TEXT COMMENT '失败原因',
    `confirm_status` TINYINT NOT NULL DEFAULT 0 COMMENT '确认状态: 0-待确认 1-已确认正常 2-已确认异常',
    `confirm_user` VARCHAR(50) COMMENT '确认人',
    `confirm_remark` VARCHAR(500) COMMENT '确认备注',
    `confirm_at` DATETIME COMMENT '确认时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_trace_id` (`trace_id`),
    INDEX `idx_rule_id` (`rule_id`),
    INDEX `idx_check_result` (`check_result`),
    INDEX `idx_confirm_status` (`confirm_status`),
    INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='核对记录表';

-- 规则版本表
CREATE TABLE IF NOT EXISTS `t_rule_version` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `rule_id` BIGINT NOT NULL COMMENT '规则ID',
    `rule_name` VARCHAR(100) NOT NULL COMMENT '规则名称',
    `method_pattern` VARCHAR(200) NOT NULL COMMENT '方法匹配模式',
    `match_type` TINYINT NOT NULL DEFAULT 1 COMMENT '匹配类型: 1-精确匹配 2-通配匹配 3-正则匹配',
    `rule_type` VARCHAR(20) NOT NULL COMMENT '规则类型: SPEL/GROOVY',
    `expression` TEXT NOT NULL COMMENT '核对表达式',
    `priority` INT NOT NULL DEFAULT 0 COMMENT '优先级',
    `enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用: 0-禁用 1-启用',
    `rule_version` INT NOT NULL DEFAULT 1 COMMENT '规则版本号',
    `status` VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT/PUBLISHED/DEPRECATED',
    `published_at` DATETIME COMMENT '发布时间',
    `created_by` VARCHAR(50) COMMENT '创建人',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_rule_id` (`rule_id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='规则版本表';
