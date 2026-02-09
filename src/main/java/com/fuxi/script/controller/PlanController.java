package com.fuxi.script.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fuxi.script.entity.ExecutionPlan;
import com.fuxi.script.entity.ExecutionPlanItem;
import com.fuxi.script.service.PlanService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/plan")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;

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
            // TODO: Load existing items
        } else {
            model.addAttribute("plan", new ExecutionPlan());
        }
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
        List<ExecutionPlanItem> items = planService.getPlanItems(planId);
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
            // We need a method in PlanService to handle verification
            // planService.verifyItem(itemId, pass, remark);
            // I'll add this method to PlanService next
            
            // For now, let's implement it directly or assume service update is coming
            // I will update PlanService first/next.
            // Wait, I should update Service first. But I can't in this tool call.
            // I'll leave a placeholder here and fix it in next steps.
            
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
}
