package com.fuxi.script.common;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class DatabaseMigration implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        log.info("Checking database schema...");

        try {
            // Check if sys_user has group_name column (old schema)
            jdbcTemplate.queryForObject("SELECT group_name FROM sys_user LIMIT 1", (rs, rowNum) -> null);
            
            log.info("Detected old schema. Migrating sys_user table...");
            
            // Add new columns
            try {
                jdbcTemplate.execute("ALTER TABLE sys_user ADD COLUMN site_id BIGINT");
            } catch (Exception e) {
                log.warn("Column addition failed (might already exist): {}", e.getMessage());
            }

            // Drop old columns
            try {
                jdbcTemplate.execute("ALTER TABLE sys_user DROP COLUMN group_name");
                jdbcTemplate.execute("ALTER TABLE sys_user DROP COLUMN site_name");
            } catch (Exception e) {
                log.warn("Column drop failed: {}", e.getMessage());
            }
            
            // Add Foreign Keys (Optional, might fail if data inconsistent, so we wrap it)
            try {
                jdbcTemplate.execute("ALTER TABLE sys_user ADD CONSTRAINT fk_user_site FOREIGN KEY (site_id) REFERENCES sys_site(id)");
            } catch (Exception e) {
                log.warn("FK addition failed: {}", e.getMessage());
            }

            log.info("Schema migration completed.");
            
        } catch (Exception e) {
            log.info("Schema appears to be up to date or query failed: {}", e.getMessage());
        }

        // Clean up Group related tables
        try {
            // Drop Foreign Key first
            try {
                jdbcTemplate.execute("ALTER TABLE sys_user DROP FOREIGN KEY fk_user_group");
            } catch (Exception ex) {
                // Ignore if not exists
            }
            
            // Drop sys_user_group first because it references sys_group
            jdbcTemplate.execute("DROP TABLE IF EXISTS sys_user_group");
            jdbcTemplate.execute("DROP TABLE IF EXISTS sys_group");
            
            // Remove group_id from sys_user if exists
            try {
                jdbcTemplate.queryForObject("SELECT group_id FROM sys_user LIMIT 1", (rs, rowNum) -> null);
                jdbcTemplate.execute("ALTER TABLE sys_user DROP COLUMN group_id");
            } catch (Exception ex) {
                // Column doesn't exist, ignore
            }
        } catch (Exception e) {
            log.warn("Failed to cleanup group tables: {}", e.getMessage());
        }

        // Check script_info target_env column
        try {
            jdbcTemplate.queryForObject("SELECT target_env FROM script_info LIMIT 1", (rs, rowNum) -> null);
        } catch (Exception e) {
            log.info("Adding missing column target_env to script_info...");
            try {
                jdbcTemplate.execute("ALTER TABLE script_info ADD COLUMN target_env VARCHAR(50) COMMENT 'MySQL, Oracle, Linux, Windows'");
            } catch (Exception ex) {
                log.warn("Failed to add target_env column: {}", ex.getMessage());
            }
        }

        // Check script_version status column
        try {
            jdbcTemplate.queryForObject("SELECT status FROM script_version LIMIT 1", (rs, rowNum) -> null);
            
            // Check audit_remark column
            try {
                jdbcTemplate.queryForObject("SELECT audit_remark FROM script_version LIMIT 1", (rs, rowNum) -> null);
            } catch (Exception ex) {
                log.info("Adding missing column audit_remark to script_version...");
                jdbcTemplate.execute("ALTER TABLE script_version ADD COLUMN audit_remark VARCHAR(500) COMMENT 'Reason for rejection'");
            }
        } catch (Exception e) {
            log.info("Adding missing column status to script_version...");
            try {
                jdbcTemplate.execute("ALTER TABLE script_version ADD COLUMN status VARCHAR(20) DEFAULT 'DRAFT' COMMENT 'DRAFT, SUBMITTED, APPROVED, REJECTED'");
                jdbcTemplate.execute("ALTER TABLE script_version ADD COLUMN audit_remark VARCHAR(500) COMMENT 'Reason for rejection'");
            } catch (Exception ex) {
                log.warn("Failed to add status column: {}", ex.getMessage());
            }
        }

        // Check sys_dict table
        try {
            jdbcTemplate.queryForObject("SELECT 1 FROM sys_dict LIMIT 1", (rs, rowNum) -> null);
            
            // Table exists, check for status column
            try {
                jdbcTemplate.queryForObject("SELECT status FROM sys_dict LIMIT 1", (rs, rowNum) -> null);
            } catch (Exception ex) {
                log.info("Adding status column to existing sys_dict table...");
                jdbcTemplate.execute("ALTER TABLE sys_dict ADD COLUMN status INT DEFAULT 0 COMMENT '0: Enabled, 1: Disabled'");
            }
            
        } catch (Exception e) {
            log.info("Creating sys_dict table...");
            try {
                jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS sys_dict (" +
                        "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                        "category VARCHAR(50) NOT NULL, " +
                        "code VARCHAR(50) NOT NULL, " +
                        "label VARCHAR(100) NOT NULL, " +
                        "sort INT DEFAULT 0, " +
                        "created_at DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                        "updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                        "status INT DEFAULT 0 COMMENT '0: Enabled, 1: Disabled', " +
                        "is_deleted INT DEFAULT 0" +
                        ")");
                
                // Init Data
                jdbcTemplate.execute("INSERT INTO sys_dict (category, code, label, sort, status) VALUES " +
                        "('script_type', 'SQL', 'SQL Script', 1, 0), " +
                        "('script_type', 'SHELL', 'Shell Script', 2, 0), " +
                        "('target_env', 'MySQL', 'MySQL Database', 1, 0), " +
                        "('target_env', 'Oracle', 'Oracle Database', 2, 0), " +
                        "('target_env', 'Linux', 'Linux Server', 3, 0), " +
                        "('target_env', 'Windows', 'Windows Server', 4, 0)");
            } catch (Exception ex) {
                log.warn("Failed to create sys_dict table: {}", ex.getMessage());
            }
        }
        
        // Check execution_plan table
        try {
            jdbcTemplate.queryForObject("SELECT 1 FROM execution_plan LIMIT 1", (rs, rowNum) -> null);
        } catch (Exception e) {
            log.info("Creating execution_plan table...");
            try {
                jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS execution_plan (" +
                        "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                        "title VARCHAR(200) NOT NULL, " +
                        "description TEXT, " +
                        "target_site VARCHAR(100), " +
                        "status VARCHAR(50) DEFAULT 'DRAFT' COMMENT 'DRAFT, PENDING, RUNNING, COMPLETED, FAILED', " +
                        "created_by VARCHAR(50), " +
                        "created_at DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                        "updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                        "is_deleted INT DEFAULT 0" +
                        ")");
            } catch (Exception ex) {
                log.warn("Failed to create execution_plan table: {}", ex.getMessage());
            }
        }

        // Check execution_plan_item table
        try {
            jdbcTemplate.queryForObject("SELECT 1 FROM execution_plan_item LIMIT 1", (rs, rowNum) -> null);
            
            // Table exists, check for status column
            try {
                jdbcTemplate.queryForObject("SELECT status FROM execution_plan_item LIMIT 1", (rs, rowNum) -> null);
            } catch (Exception ex) {
                log.info("Adding missing columns to execution_plan_item table...");
                jdbcTemplate.execute("ALTER TABLE execution_plan_item ADD COLUMN status VARCHAR(50) DEFAULT 'PENDING' COMMENT 'PENDING, RUNNING, SUCCESS, FAILED'");
                jdbcTemplate.execute("ALTER TABLE execution_plan_item ADD COLUMN execution_result TEXT");
                jdbcTemplate.execute("ALTER TABLE execution_plan_item ADD COLUMN started_at DATETIME");
                jdbcTemplate.execute("ALTER TABLE execution_plan_item ADD COLUMN finished_at DATETIME");
            }
            
            // Check verify_status column
            try {
                jdbcTemplate.queryForObject("SELECT verify_status FROM execution_plan_item LIMIT 1", (rs, rowNum) -> null);
            } catch (Exception ex) {
                log.info("Adding verification columns to execution_plan_item table...");
                jdbcTemplate.execute("ALTER TABLE execution_plan_item ADD COLUMN verify_status VARCHAR(50) DEFAULT 'PENDING' COMMENT 'PENDING, PASS, FAIL'");
                jdbcTemplate.execute("ALTER TABLE execution_plan_item ADD COLUMN verify_remark TEXT");
                jdbcTemplate.execute("ALTER TABLE execution_plan_item ADD COLUMN verified_by VARCHAR(50)");
                jdbcTemplate.execute("ALTER TABLE execution_plan_item ADD COLUMN verified_at DATETIME");
            }
            
        } catch (Exception e) {
            log.info("Creating execution_plan_item table...");
            try {
                jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS execution_plan_item (" +
                        "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                        "plan_id BIGINT NOT NULL, " +
                        "script_version_id BIGINT NOT NULL, " +
                        "sort_order INT DEFAULT 0, " +
                        "status VARCHAR(50) DEFAULT 'PENDING' COMMENT 'PENDING, RUNNING, SUCCESS, FAILED', " +
                        "execution_result TEXT, " +
                        "started_at DATETIME, " +
                        "finished_at DATETIME, " +
                        "verify_status VARCHAR(50) DEFAULT 'PENDING' COMMENT 'PENDING, PASS, FAIL', " +
                        "verify_remark TEXT, " +
                        "verified_by VARCHAR(50), " +
                        "verified_at DATETIME, " +
                        "FOREIGN KEY (plan_id) REFERENCES execution_plan(id)" +
                        ")");
            } catch (Exception ex) {
                log.warn("Failed to create execution_plan_item table: {}", ex.getMessage());
            }
        }
    }
}
