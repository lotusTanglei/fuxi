package com.fuxi.script.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("script_version")
public class ScriptVersion {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long scriptId;
    private String versionNum;
    private String content;
    private String remark;
    private String status; // DRAFT, SUBMITTED, APPROVED, REJECTED
    private String auditRemark;
    private Long assignedLeaderId; // ID of the leader assigned to audit
    
    private String createdBy;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
