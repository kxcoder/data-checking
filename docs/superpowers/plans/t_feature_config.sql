CREATE TABLE t_feature_config (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    feature_key    VARCHAR(64) NOT NULL UNIQUE COMMENT '特征唯一标识',
    feature_name   VARCHAR(128) COMMENT '特征名称',
    supplier_type  VARCHAR(16) NOT NULL COMMENT '数据源类型: HTTP/RPC/DB/REDIS',
    supplier_key   VARCHAR(64) NOT NULL COMMENT '关联的DataSupplier key',
    transform_type VARCHAR(16) DEFAULT 'NONE' COMMENT '转换类型: NONE/SPEL/GROOVY',
    transform_script TEXT COMMENT '清洗脚本',
    extract_path   VARCHAR(128) COMMENT 'JSON提取路径',
    cache_ttl      INT DEFAULT -1 COMMENT '缓存TTL(秒): -1不缓存, 0永久, >0按TTL',
    priority       INT DEFAULT 0 COMMENT '优先级',
    enabled        TINYINT DEFAULT 1 COMMENT '是否启用',
    created_at     DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_feature_key (feature_key),
    INDEX idx_enabled (enabled)
) COMMENT '特征配置表';
