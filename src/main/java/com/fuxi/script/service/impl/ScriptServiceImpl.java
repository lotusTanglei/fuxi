package com.fuxi.script.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fuxi.script.entity.ScriptInfo;
import com.fuxi.script.entity.ScriptVersion;
import com.fuxi.script.entity.SysUser;
import com.fuxi.script.mapper.ScriptInfoMapper;
import com.fuxi.script.mapper.ScriptVersionMapper;
import com.fuxi.script.mapper.SysUserMapper;
import com.fuxi.script.service.ScriptService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ScriptServiceImpl extends ServiceImpl<ScriptInfoMapper, ScriptInfo> implements ScriptService {

    private final ScriptVersionMapper scriptVersionMapper;
    private final SysUserMapper sysUserMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createScript(ScriptInfo scriptInfo, String content, String remark) {
        // 1. Save Script Info
        String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();
        scriptInfo.setCreatedBy(currentUser);
        this.save(scriptInfo);

        // 2. Create Initial Version 1.0
        ScriptVersion version = new ScriptVersion();
        version.setScriptId(scriptInfo.getId());
        version.setVersionNum("1.0");
        version.setContent(content);
        version.setRemark(remark != null ? remark : "Initial Version");
        version.setStatus("DRAFT");
        version.setCreatedBy(currentUser);
        version.setCreatedAt(LocalDateTime.now());
        
        scriptVersionMapper.insert(version);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createNewVersion(Long scriptId, String content, String remark) {
        // 1. Get Latest Version
        ScriptVersion latest = getLatestVersion(scriptId);
        String nextVersion = "1.0";
        if (latest != null) {
            nextVersion = incrementVersion(latest.getVersionNum());
        }

        // 2. Create New Version
        String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();
        ScriptVersion version = new ScriptVersion();
        version.setScriptId(scriptId);
        version.setVersionNum(nextVersion);
        version.setContent(content);
        version.setRemark(remark);
        version.setStatus("DRAFT");
        version.setCreatedBy(currentUser);
        version.setCreatedAt(LocalDateTime.now());
        
        scriptVersionMapper.insert(version);
        
        // 3. Update Script Info updated_at
        ScriptInfo scriptInfo = this.getById(scriptId);
        if (scriptInfo != null) {
            this.updateById(scriptInfo); // Will trigger auto-fill for updatedAt
        }
    }

    @Override
    public ScriptVersion getLatestVersion(Long scriptId) {
        return scriptVersionMapper.selectOne(new LambdaQueryWrapper<ScriptVersion>()
                .eq(ScriptVersion::getScriptId, scriptId)
                .orderByDesc(ScriptVersion::getId)
                .last("LIMIT 1"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateScript(Long scriptId, ScriptInfo scriptInfo, String content, String remark) {
        // Update ScriptInfo
        scriptInfo.setId(scriptId);
        this.updateById(scriptInfo);

        // ALWAYS create a new version for every save
        createNewVersion(scriptId, content, remark);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitScript(Long scriptId, Long leaderId) {
        ScriptVersion latest = getLatestVersion(scriptId);
        if (latest != null) {
            latest.setStatus("SUBMITTED");
            latest.setAssignedLeaderId(leaderId);
            scriptVersionMapper.updateById(latest);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewScript(Long scriptId, boolean pass, String remark) {
        ScriptVersion latest = getLatestVersion(scriptId);
        if (latest != null) {
            latest.setStatus(pass ? "APPROVED" : "REJECTED");
            latest.setAuditRemark(remark);
            scriptVersionMapper.updateById(latest);
        }
    }

    @Override
    public com.baomidou.mybatisplus.extension.plugins.pagination.Page<ScriptVersion> getPendingAuditVersions(com.baomidou.mybatisplus.extension.plugins.pagination.Page<ScriptVersion> page) {
        LambdaQueryWrapper<ScriptVersion> wrapper = new LambdaQueryWrapper<ScriptVersion>()
                .eq(ScriptVersion::getStatus, "SUBMITTED")
                .orderByDesc(ScriptVersion::getId);

        // Filter by Assigned Leader if current user is LEADER (and not ADMIN)
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        SysUser currentUser = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, currentUsername));

        if (currentUser != null && "LEADER".equals(currentUser.getRole())) {
            wrapper.eq(ScriptVersion::getAssignedLeaderId, currentUser.getId());
        }

        return scriptVersionMapper.selectPage(page, wrapper);
    }

    @Override
    public java.util.List<ScriptVersion> getScriptVersions(Long scriptId) {
        return scriptVersionMapper.selectList(new LambdaQueryWrapper<ScriptVersion>()
                .eq(ScriptVersion::getScriptId, scriptId)
                .orderByDesc(ScriptVersion::getId));
    }

    @Override
    public ScriptVersion getScriptVersion(Long versionId) {
        return scriptVersionMapper.selectById(versionId);
    }

    private String incrementVersion(String version) {
        try {
            String[] parts = version.split("\\.");
            if (parts.length == 2) {
                int major = Integer.parseInt(parts[0]);
                int minor = Integer.parseInt(parts[1]);
                return major + "." + (minor + 1);
            }
        } catch (Exception e) {
            // Fallback
        }
        return version + ".1";
    }
}
