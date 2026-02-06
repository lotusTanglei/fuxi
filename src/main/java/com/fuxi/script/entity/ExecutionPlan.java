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
    private String status; // DRAFT, PENDING, APPROVED, REJECTED, COMPLETED
    
    private String createdBy;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    
    @TableLogic
    private Integer isDeleted;
}
