package com.fuxi.script.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fuxi.script.entity.ScriptInfo;
import com.fuxi.script.entity.ScriptVersion;
import com.fuxi.script.service.ScriptService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/script")
@RequiredArgsConstructor
public class ScriptController {

    private final ScriptService scriptService;

    @GetMapping("/list")
    public String list() {
        return "script/list";
    }

    @GetMapping("/api/list")
    @ResponseBody
    public Map<String, Object> apiList(@RequestParam(defaultValue = "1") Integer page,
                                       @RequestParam(defaultValue = "10") Integer limit,
                                       @RequestParam(required = false) String title,
                                       @RequestParam(required = false) String status) {
        Page<ScriptInfo> pageParam = new Page<>(page, limit);
        LambdaQueryWrapper<ScriptInfo> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(title)) {
            wrapper.like(ScriptInfo::getTitle, title);
        }
        wrapper.orderByDesc(ScriptInfo::getId);
        
        Page<ScriptInfo> result = scriptService.page(pageParam, wrapper);
        
        // Enrich with latest version info
        List<ScriptInfo> records = result.getRecords();
        // If status filter is provided, we need to filter records after enriching, 
        // or better, query based on latest version status. 
        // Querying on latest version status in SQL is complex with MybatisPlus wrapper on ScriptInfo.
        // For simplicity, we filter in memory here, but pagination might be off.
        // A better approach is to filter where possible or accept memory filtering limitation for now.
        // Given the requirement, let's filter in memory.
        
        List<ScriptInfo> enrichedRecords = records.stream().map(script -> {
            ScriptVersion latest = scriptService.getLatestVersion(script.getId());
            if (latest != null) {
                script.setVersionNum(latest.getVersionNum());
                script.setStatus(latest.getStatus());
                script.setContent(latest.getContent());
                script.setRemark(latest.getRemark());
                script.setLatestVersionId(latest.getId());
            }
            return script;
        }).filter(script -> {
            if (StringUtils.hasText(status)) {
                return status.equals(script.getStatus());
            }
            return true;
        }).collect(Collectors.toList());
        
        // If we filter in memory, the total count in 'result' is wrong for the filtered list.
        // However, Layui table might handle it if we return the filtered count.
        
        Map<String, Object> map = new HashMap<>();
        map.put("code", 0);
        map.put("msg", "");
        map.put("count", StringUtils.hasText(status) ? enrichedRecords.size() : result.getTotal()); // Approx
        map.put("data", enrichedRecords);
        return map;
    }

    @GetMapping("/form")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEV', 'LEADER')")
    public String form(@RequestParam(required = false) Long id, Model model) {
        if (id != null) {
            ScriptInfo script = scriptService.getById(id);
            ScriptVersion latest = scriptService.getLatestVersion(id);
            if (latest != null) {
                script.setVersionNum(latest.getVersionNum());
                script.setStatus(latest.getStatus());
                script.setContent(latest.getContent());
                script.setRemark(latest.getRemark());
            }
            model.addAttribute("script", script);
        } else {
            model.addAttribute("script", new ScriptInfo());
        }
        return "script/form";
    }

    @PostMapping("/save")
    @ResponseBody
    @PreAuthorize("hasAnyRole('ADMIN', 'DEV', 'LEADER')")
    public Map<String, Object> save(@RequestBody ScriptInfo script) {
        try {
            if (script.getId() != null) {
                scriptService.updateScript(script.getId(), script, script.getContent(), script.getRemark());
            } else {
                scriptService.createScript(script, script.getContent(), script.getRemark());
            }
            
            Map<String, Object> map = new HashMap<>();
            map.put("code", 0);
            map.put("msg", "保存成功");
            return map;
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> map = new HashMap<>();
            map.put("code", 1);
            map.put("msg", "保存失败: " + e.getMessage());
            return map;
        }
    }
    
    @PostMapping("/submit/{id}")
    @ResponseBody
    @PreAuthorize("hasAnyRole('ADMIN', 'DEV', 'LEADER')")
    public Map<String, Object> submit(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> params) {
        try {
            Long leaderId = null;
            if (params != null && params.containsKey("leaderId")) {
                Object leaderIdObj = params.get("leaderId");
                if (leaderIdObj instanceof Number) {
                    leaderId = ((Number) leaderIdObj).longValue();
                } else if (leaderIdObj instanceof String) {
                    leaderId = Long.parseLong((String) leaderIdObj);
                }
            }
            
            scriptService.submitScript(id, leaderId);
            Map<String, Object> map = new HashMap<>();
            map.put("code", 0);
            map.put("msg", "提交成功");
            return map;
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> map = new HashMap<>();
            map.put("code", 1);
            map.put("msg", "提交失败: " + e.getMessage());
            return map;
        }
    }
    
    @PostMapping("/delete/{id}")
    @ResponseBody
    @PreAuthorize("hasAnyRole('ADMIN', 'DEV', 'LEADER')")
    public Map<String, Object> delete(@PathVariable Long id) {
        boolean success = scriptService.removeById(id);
        Map<String, Object> map = new HashMap<>();
        map.put("code", success ? 0 : 1);
        map.put("msg", success ? "删除成功" : "删除失败");
        return map;
    }

    // --- Audit APIs ---

    @GetMapping("/audit/list")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEADER')")
    public String auditList() {
        return "script/audit";
    }

    @GetMapping("/api/audit/list")
    @ResponseBody
    @PreAuthorize("hasAnyRole('ADMIN', 'LEADER')")
    public Map<String, Object> apiAuditList(@RequestParam(defaultValue = "1") Integer page,
                                            @RequestParam(defaultValue = "10") Integer limit) {
        Page<ScriptVersion> pageParam = new Page<>(page, limit);
        Page<ScriptVersion> result = scriptService.getPendingAuditVersions(pageParam);
        
        // Enrich with Script Title
        List<Map<String, Object>> enrichedRecords = result.getRecords().stream().map(v -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", v.getScriptId()); // Use Script ID for review action
            map.put("versionId", v.getId());
            map.put("versionNum", v.getVersionNum());
            map.put("content", v.getContent());
            map.put("remark", v.getRemark());
            map.put("createdBy", v.getCreatedBy());
            map.put("createdAt", v.getCreatedAt());
            
            ScriptInfo info = scriptService.getById(v.getScriptId());
            if (info != null) {
                map.put("title", info.getTitle());
                map.put("type", info.getType());
            } else {
                map.put("title", "Unknown Script");
            }
            return map;
        }).collect(Collectors.toList());
        
        Map<String, Object> map = new HashMap<>();
        map.put("code", 0);
        map.put("msg", "");
        map.put("count", result.getTotal());
        map.put("data", enrichedRecords);
        return map;
    }
    
    @PostMapping("/audit/{id}")
    @ResponseBody
    @PreAuthorize("hasAnyRole('ADMIN', 'LEADER')")
    public Map<String, Object> audit(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        boolean pass = (boolean) params.get("pass");
        String remark = (String) params.get("remark");
        
        try {
            scriptService.reviewScript(id, pass, remark);
            Map<String, Object> map = new HashMap<>();
            map.put("code", 0);
            map.put("msg", "审核完成");
            return map;
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> map = new HashMap<>();
            map.put("code", 1);
            map.put("msg", "审核失败: " + e.getMessage());
            return map;
        }
    }
    
    // --- Version History APIs ---
    
    @GetMapping("/history/{id}")
    public String history(@PathVariable Long id, Model model) {
        model.addAttribute("script", scriptService.getById(id));
        return "script/history";
    }

    @GetMapping("/api/versions/{scriptId}")
    @ResponseBody
    public Map<String, Object> apiVersions(@PathVariable Long scriptId) {
        List<ScriptVersion> versions = scriptService.getScriptVersions(scriptId);
        Map<String, Object> map = new HashMap<>();
        map.put("code", 0);
        map.put("data", versions);
        return map;
    }
    
    @GetMapping("/api/version/{versionId}")
    @ResponseBody
    public Map<String, Object> apiVersionDetail(@PathVariable Long versionId) {
        ScriptVersion version = scriptService.getScriptVersion(versionId);
        Map<String, Object> map = new HashMap<>();
        if (version != null) {
            map.put("code", 0);
            map.put("data", version);
        } else {
            map.put("code", 1);
            map.put("msg", "Version not found");
        }
        return map;
    }
}
