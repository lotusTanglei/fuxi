package com.fuxi.script.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("execution_feedback")
public class ExecutionFeedback {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long planItemId;
    private String executionStatus; // SUCCESS, FAIL, SKIPPED
    private String reviewStatus; // PENDING, APPROVED, REJECTED
    private String logContent;
    private LocalDateTime executionTime;
    private String executedBy;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
