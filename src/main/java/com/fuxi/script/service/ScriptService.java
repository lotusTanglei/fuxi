package com.fuxi.script.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.fuxi.script.entity.ScriptInfo;
import com.fuxi.script.entity.ScriptVersion;

public interface ScriptService extends IService<ScriptInfo> {
    
    /**
     * Create a new script with initial version 1.0
     */
    void createScript(ScriptInfo scriptInfo, String content, String remark);

    /**
     * Create a new version for an existing script
     */
    void createNewVersion(Long scriptId, String content, String remark);
    
    /**
     * Get the latest version of a script
     */
    ScriptVersion getLatestVersion(Long scriptId);

    /**
     * Update script content (updates latest version if it's in editable state)
     */
    void updateScript(Long scriptId, ScriptInfo scriptInfo, String content, String remark);

    /**
     * Submit script for review
     */
    void submitScript(Long scriptId);
    
    /**
     * Review script
     */
    void reviewScript(Long scriptId, boolean pass, String remark);
    
    /**
     * Get pending audit versions
     */
    com.baomidou.mybatisplus.extension.plugins.pagination.Page<ScriptVersion> getPendingAuditVersions(com.baomidou.mybatisplus.extension.plugins.pagination.Page<ScriptVersion> page);
    
    /**
     * Get all versions of a script
     */
    java.util.List<ScriptVersion> getScriptVersions(Long scriptId);
    
    /**
     * Get specific version
     */
    ScriptVersion getScriptVersion(Long versionId);
}
