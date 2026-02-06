package com.fuxi.script.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fuxi.script.entity.SysDict;
import com.fuxi.script.service.SysDictService;
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
@RequestMapping("/dict")
@RequiredArgsConstructor
public class DictController {

    private final SysDictService sysDictService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/list")
    public String list() {
        return "dict/list";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/list")
    @ResponseBody
    public Map<String, Object> apiList(@RequestParam(defaultValue = "1") Integer page,
                                       @RequestParam(defaultValue = "10") Integer limit,
                                       @RequestParam(required = false) String category,
                                       @RequestParam(required = false) String label) {
        Page<SysDict> pageParam = new Page<>(page, limit);
        LambdaQueryWrapper<SysDict> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(category)) {
            wrapper.eq(SysDict::getCategory, category);
        }
        if (StringUtils.hasText(label)) {
            wrapper.like(SysDict::getLabel, label);
        }
        wrapper.orderByAsc(SysDict::getCategory).orderByAsc(SysDict::getSort);
        
        Page<SysDict> result = sysDictService.page(pageParam, wrapper);
        
        Map<String, Object> map = new HashMap<>();
        map.put("code", 0);
        map.put("msg", "");
        map.put("count", result.getTotal());
        map.put("data", result.getRecords());
        return map;
    }
    
    @GetMapping("/api/category/{category}")
    @ResponseBody
    public Map<String, Object> getByCategory(@PathVariable String category) {
        List<SysDict> list = sysDictService.getDictsByCategory(category);
        Map<String, Object> map = new HashMap<>();
        map.put("code", 0);
        map.put("data", list);
        return map;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/form")
    public String form(@RequestParam(required = false) Long id, Model model) {
        if (id != null) {
            model.addAttribute("dict", sysDictService.getById(id));
        } else {
            model.addAttribute("dict", new SysDict());
        }
        return "dict/form";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/save")
    @ResponseBody
    public Map<String, Object> save(@RequestBody SysDict dict) {
        try {
            if (dict.getId() != null) {
                sysDictService.updateById(dict);
            } else {
                sysDictService.save(dict);
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
    
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/delete/{id}")
    @ResponseBody
    public Map<String, Object> delete(@PathVariable Long id) {
        boolean success = sysDictService.removeById(id);
        Map<String, Object> map = new HashMap<>();
        map.put("code", success ? 0 : 1);
        map.put("msg", success ? "删除成功" : "删除失败");
        return map;
    }
}
