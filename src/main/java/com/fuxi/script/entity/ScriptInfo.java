package com.fuxi.script.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("script_info")
public class ScriptInfo {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String title;
    private String description;
    private String type; // SQL, SHELL
    private String targetEnv; // MySQL, Oracle, Linux, Windows
    
    private String createdBy;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    
    @TableLogic
    private Integer isDeleted;
    
    // Extra fields for UI/DTO
    @TableField(exist = false)
    private String versionNum;
    
    @TableField(exist = false)
    private String content;
    
    @TableField(exist = false)
    private String status;
    
    @TableField(exist = false)
    private String remark;
    
    @TableField(exist = false)
    private Long latestVersionId;
    
    @TableField(exist = false)
    private String creatorRealName;
}
