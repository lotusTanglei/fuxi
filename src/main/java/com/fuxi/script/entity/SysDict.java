package com.fuxi.script.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_dict")
public class SysDict {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String category; // e.g., 'script_type', 'target_env'
    private String code;
    private String label;
    private Integer sort;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    
    // 0: Enabled, 1: Disabled
    private Integer status;
    
    @TableLogic
    private Integer isDeleted;
}
