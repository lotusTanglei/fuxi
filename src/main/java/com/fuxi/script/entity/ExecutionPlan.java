package com.fuxi.script.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("execution_plan")
public class ExecutionPlan {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String title;
    private String description;
    private String targetSite;
    private String status; // DRAFT, PENDING, RUNNING, VERIFYING_TEST, VERIFYING_LEADER, COMPLETED
    
    // Assigned Roles
    private Long assignedOpsId;
    private Long assignedLeaderId;
    private Long assignedTestId;
    
    private String executionReceipt; // OPS execution receipt
    
    private String createdBy;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    
    @TableLogic
    private Integer isDeleted;
}
