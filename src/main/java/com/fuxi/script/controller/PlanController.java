package com.fuxi.script.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fuxi.script.entity.ExecutionPlan;
import com.fuxi.script.entity.ExecutionPlanItem;
import com.fuxi.script.service.PlanService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fuxi.script.entity.SysUser;
import com.fuxi.script.service.SysUserService;

@Controller
@RequestMapping("/plan")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;
    private final SysUserService sysUserService;

    @GetMapping("/list")
    public String list() {
        return "plan/list";
    }

    @GetMapping("/api/list")
    @ResponseBody
    public Map<String, Object> apiList(@RequestParam(defaultValue = "1") Integer page,
                                       @RequestParam(defaultValue = "10") Integer limit,
                                       @RequestParam(required = false) String title) {
        Page<ExecutionPlan> pageParam = new Page<>(page, limit);
        LambdaQueryWrapper<ExecutionPlan> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(title)) {
            wrapper.like(ExecutionPlan::getTitle, title);
        }
        wrapper.orderByDesc(ExecutionPlan::getId);
        
        Page<ExecutionPlan> result = planService.page(pageParam, wrapper);
        
        Map<String, Object> map = new HashMap<>();
        map.put("code", 0);
        map.put("msg", "");
        map.put("count", result.getTotal());
        map.put("data", result.getRecords());
        return map;
    }

    @GetMapping("/form")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEADER')")
    public String form(@RequestParam(required = false) Long id, Model model) {
        if (id != null) {
            model.addAttribute("plan", planService.getById(id));
        } else {
            model.addAttribute("plan", new ExecutionPlan());
        }
        
        // Load candidates for roles
        model.addAttribute("opsUsers", sysUserService.list(new LambdaQueryWrapper<SysUser>().like(SysUser::getRole, "OPS")));
        model.addAttribute("leaderUsers", sysUserService.list(new LambdaQueryWrapper<SysUser>().like(SysUser::getRole, "LEADER")));
        model.addAttribute("testUsers", sysUserService.list(new LambdaQueryWrapper<SysUser>().like(SysUser::getRole, "TEST")));
        
        return "plan/form";
    }

    @Data
    static class PlanRequest {
        private ExecutionPlan plan;
        private List<Long> scriptVersionIds;
    }

    @PostMapping("/save")
    @ResponseBody
    @PreAuthorize("hasAnyRole('ADMIN', 'LEADER')")
    public Map<String, Object> save(@RequestBody PlanRequest request) {
        try {
            if (request.getPlan().getId() != null) {
                planService.updatePlan(request.getPlan(), request.getScriptVersionIds());
            } else {
                planService.createPlan(request.getPlan(), request.getScriptVersionIds());
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
    
    @GetMapping("/detail/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("plan", planService.getById(id));
        return "plan/detail";
    }
    
    @GetMapping("/api/items/{planId}")
    @ResponseBody
    public Map<String, Object> getItems(@PathVariable Long planId) {
        List<Map<String, Object>> items = planService.getPlanItemsWithDetails(planId);
        Map<String, Object> map = new HashMap<>();
        map.put("code", 0);
        map.put("data", items);
        return map;
    }

    @PostMapping("/execute/{id}")
    @ResponseBody
    @PreAuthorize("hasAnyRole('ADMIN', 'OPS')")
    public Map<String, Object> execute(@PathVariable Long id) {
        try {
            // Check assignment permissions
            ExecutionPlan plan = planService.getById(id);
            if (plan != null) {
                String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
                SysUser currentUser = sysUserService.getOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, currentUsername));
                
                boolean isAssignedOps = plan.getAssignedOpsId() != null && plan.getAssignedOpsId().equals(currentUser.getId());
                boolean isAdmin = "ADMIN".equals(currentUser.getRole()) || currentUser.getRole().contains("ADMIN");
                
                if (!isAssignedOps && !isAdmin) {
                    throw new RuntimeException("Permission Denied: You are not the assigned OPS for this plan.");
                }
            }
            
            planService.executePlan(id);
            Map<String, Object> map = new HashMap<>();
            map.put("code", 0);
            map.put("msg", "任务已开始执行");
            return map;
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> map = new HashMap<>();
            map.put("code", 1);
            map.put("msg", "执行失败: " + e.getMessage());
            return map;
        }
    }
    
    @PostMapping("/verify/{itemId}")
    @ResponseBody
    @PreAuthorize("hasAnyRole('ADMIN', 'LEADER', 'TEST')")
    public Map<String, Object> verify(@PathVariable Long itemId, @RequestBody Map<String, Object> params) {
        boolean pass = (boolean) params.get("pass");
        String remark = (String) params.get("remark");
        
        try {
            planService.verifyItem(itemId, pass, remark);
            
            Map<String, Object> map = new HashMap<>();
            map.put("code", 0);
            map.put("msg", "验证完成");
            return map;
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> map = new HashMap<>();
            map.put("code", 1);
            map.put("msg", "验证失败: " + e.getMessage());
            return map;
        }
    }

    @GetMapping("/my")
    public String myTodo() {
        return "plan/my";
    }

    @GetMapping("/api/my")
    @ResponseBody
    public Map<String, Object> apiMyTodo(@RequestParam(defaultValue = "1") Integer page,
                                         @RequestParam(defaultValue = "10") Integer limit) {
        // ... (implementation same as before) ...
        // Get current user
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        SysUser currentUser = sysUserService.getOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, currentUsername));
        
        Page<ExecutionPlan> pageParam = new Page<>(page, limit);
        LambdaQueryWrapper<ExecutionPlan> wrapper = new LambdaQueryWrapper<>();
        
        // Filter based on Role and Status
        if ("OPS".equals(currentUser.getRole())) {
            wrapper.eq(ExecutionPlan::getAssignedOpsId, currentUser.getId())
                   .in(ExecutionPlan::getStatus, "PENDING", "RUNNING");
        } else if ("TEST".equals(currentUser.getRole())) {
            wrapper.eq(ExecutionPlan::getAssignedTestId, currentUser.getId())
                   .eq(ExecutionPlan::getStatus, "VERIFYING_TEST");
        } else if ("LEADER".equals(currentUser.getRole())) {
            wrapper.eq(ExecutionPlan::getAssignedLeaderId, currentUser.getId())
                   .eq(ExecutionPlan::getStatus, "VERIFYING_LEADER");
        } else if ("ADMIN".equals(currentUser.getRole())) {
             // ADMIN sees everything
        } else {
             wrapper.eq(ExecutionPlan::getId, -1);
        }
        
        wrapper.orderByDesc(ExecutionPlan::getId);
        
        Page<ExecutionPlan> result = planService.page(pageParam, wrapper);
        
        Map<String, Object> map = new HashMap<>();
        map.put("code", 0);
        map.put("msg", "");
        map.put("count", result.getTotal());
        map.put("data", result.getRecords());
        return map;
    }
    
    @PostMapping("/receipt/{id}")
    @ResponseBody
    @PreAuthorize("hasAnyRole('ADMIN', 'OPS')")
    public Map<String, Object> submitReceipt(@PathVariable Long id, @RequestBody Map<String, String> params) {
        String receipt = params.get("receipt");
        try {
            planService.submitReceipt(id, receipt);
            Map<String, Object> map = new HashMap<>();
            map.put("code", 0);
            map.put("msg", "回执提交成功");
            return map;
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> map = new HashMap<>();
            map.put("code", 1);
            map.put("msg", "提交失败: " + e.getMessage());
            return map;
        }
    }
    
    @PostMapping("/verify/test/{id}")
    @ResponseBody
    @PreAuthorize("hasAnyRole('ADMIN', 'TEST')")
    public Map<String, Object> verifyTest(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        boolean pass = (boolean) params.get("pass");
        try {
            planService.verifyPlanTest(id, pass);
            Map<String, Object> map = new HashMap<>();
            map.put("code", 0);
            map.put("msg", "验证完成");
            return map;
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> map = new HashMap<>();
            map.put("code", 1);
            map.put("msg", "验证失败: " + e.getMessage());
            return map;
        }
    }
    
    @PostMapping("/finalize/{id}")
    @ResponseBody
    @PreAuthorize("hasAnyRole('ADMIN', 'LEADER')")
    public Map<String, Object> finalizePlan(@PathVariable Long id) {
        try {
            planService.finalizePlan(id);
            Map<String, Object> map = new HashMap<>();
            map.put("code", 0);
            map.put("msg", "结项完成");
            return map;
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> map = new HashMap<>();
            map.put("code", 1);
            map.put("msg", "结项失败: " + e.getMessage());
            return map;
        }
    }
}