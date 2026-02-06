package com.fuxi.script.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("execution_plan_item")
public class ExecutionPlanItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long planId;
    private Long scriptVersionId;
    private Integer sortOrder;
    
    private String status; // PENDING, RUNNING, SUCCESS, FAILED
    private String executionResult;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    
    private String verifyStatus; // PENDING, PASS, FAIL
    private String verifyRemark;
    private String verifiedBy;
    private LocalDateTime verifiedAt;
}
