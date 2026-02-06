-- 现场表
CREATE TABLE IF NOT EXISTS sys_site (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(200),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT DEFAULT 0
);

-- 用户组表
CREATE TABLE IF NOT EXISTS sys_group (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(200),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT DEFAULT 0
);

-- 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    real_name VARCHAR(50),
    role VARCHAR(20) NOT NULL COMMENT 'ADMIN, DEV, OPS, TEST',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT DEFAULT 0
);

-- 用户-现场关联表
CREATE TABLE IF NOT EXISTS sys_user_site (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    site_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_site (user_id, site_id),
    FOREIGN KEY (user_id) REFERENCES sys_user(id),
    FOREIGN KEY (site_id) REFERENCES sys_site(id)
);

-- 用户-用户组关联表
CREATE TABLE IF NOT EXISTS sys_user_group (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    group_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_group (user_id, group_id),
    FOREIGN KEY (user_id) REFERENCES sys_user(id),
    FOREIGN KEY (group_id) REFERENCES sys_group(id)
);

-- 脚本信息表
CREATE TABLE IF NOT EXISTS script_info (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    type VARCHAR(20) NOT NULL COMMENT 'SQL, SHELL',
    target_env VARCHAR(50) COMMENT 'MySQL, Oracle, Linux, Windows',
    created_by VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT DEFAULT 0
);

-- 脚本版本表
CREATE TABLE IF NOT EXISTS script_version (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    script_id BIGINT NOT NULL,
    version_num VARCHAR(20) NOT NULL COMMENT 'V1, V2...',
    content MEDIUMTEXT,
    remark VARCHAR(200),
    status VARCHAR(20) DEFAULT 'DRAFT' COMMENT 'DRAFT, SUBMITTED, APPROVED, REJECTED',
    created_by VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (script_id) REFERENCES script_info(id)
);

-- 执行任务包表
CREATE TABLE IF NOT EXISTS execution_plan (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    target_site VARCHAR(50),
    status VARCHAR(20) DEFAULT 'DRAFT' COMMENT 'DRAFT, DISPATCHED, COMPLETED',
    created_by VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT DEFAULT 0
);

-- 执行任务包明细表
CREATE TABLE IF NOT EXISTS execution_plan_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_id BIGINT NOT NULL,
    script_version_id BIGINT NOT NULL,
    sort_order INT DEFAULT 0,
    FOREIGN KEY (plan_id) REFERENCES execution_plan(id),
    FOREIGN KEY (script_version_id) REFERENCES script_version(id)
);

-- 执行反馈表
CREATE TABLE IF NOT EXISTS execution_feedback (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_item_id BIGINT NOT NULL,
    execution_status VARCHAR(20) COMMENT 'SUCCESS, FAIL, SKIPPED',
    review_status VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING, APPROVED, REJECTED',
    log_content TEXT,
    execution_time TIMESTAMP,
    executed_by VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (plan_item_id) REFERENCES execution_plan_item(id)
);
