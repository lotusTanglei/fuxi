package com.fuxi.script.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fuxi.script.entity.SysSite;
import com.fuxi.script.service.SysSiteService;
import com.fuxi.script.service.SysUserService;
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
@RequestMapping("/site")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'LEADER')") // Allow LEADER to access site list
public class SiteController {

    private final SysSiteService sysSiteService;
    private final SysUserService sysUserService;

    @GetMapping("/list")
    public String list() {
        return "site/list";
    }

    @GetMapping("/api/list")
    @ResponseBody
    @PreAuthorize("hasAnyRole('ADMIN', 'LEADER', 'OPS', 'DEV', 'TEST')") // Allow all roles to fetch site list for dropdowns
    public Map<String, Object> apiList(@RequestParam(defaultValue = "1") Integer page,
                                       @RequestParam(defaultValue = "10") Integer limit,
                                       @RequestParam(required = false) String name) {
        Page<SysSite> pageParam = new Page<>(page, limit);
        LambdaQueryWrapper<SysSite> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(name)) {
            wrapper.like(SysSite::getName, name);
        }
        wrapper.orderByDesc(SysSite::getId);
        
        Page<SysSite> result = sysSiteService.page(pageParam, wrapper);
        
        Map<String, Object> map = new HashMap<>();
        map.put("code", 0);
        map.put("msg", "");
        map.put("count", result.getTotal());
        map.put("data", result.getRecords());
        return map;
    }

    @GetMapping("/form")
    public String form(@RequestParam(required = false) Long id, Model model) {
        if (id != null) {
            SysSite site = sysSiteService.getById(id);
            model.addAttribute("site", site);
        } else {
            model.addAttribute("site", new SysSite());
        }
        return "site/form";
    }

    @PostMapping("/save")
    @ResponseBody
    public Map<String, Object> save(@RequestBody SysSite site) {
        boolean success;
        if (site.getId() != null) {
            success = sysSiteService.updateById(site);
        } else {
            success = sysSiteService.save(site);
        }
        
        Map<String, Object> map = new HashMap<>();
        map.put("code", success ? 0 : 1);
        map.put("msg", success ? "保存成功" : "保存失败");
        return map;
    }
    
    @PostMapping("/delete/{id}")
    @ResponseBody
    public Map<String, Object> delete(@PathVariable Long id) {
        boolean success = sysSiteService.removeById(id);
        Map<String, Object> map = new HashMap<>();
        map.put("code", success ? 0 : 1);
        map.put("msg", success ? "删除成功" : "删除失败");
        return map;
    }

    @GetMapping("/users")
    public String users(@RequestParam Long id, Model model) {
        model.addAttribute("siteId", id);
        model.addAttribute("allUsers", sysUserService.list());
        model.addAttribute("selectedUserIds", sysUserService.getUserIdsBySiteId(id));
        return "site/users";
    }

    @PostMapping("/users/save")
    @ResponseBody
    public Map<String, Object> saveUsers(@RequestBody Map<String, Object> params) {
        Long siteId = Long.valueOf(params.get("siteId").toString());
        
        List<Long> userIds = new java.util.ArrayList<>();
        if (params.get("userIds") != null) {
            List<?> rawUserIds = (List<?>) params.get("userIds");
            userIds = rawUserIds.stream()
                .map(id -> Long.valueOf(id.toString()))
                .collect(java.util.stream.Collectors.toList());
        }
        
        boolean success = sysUserService.configSiteUsers(siteId, userIds);
        Map<String, Object> map = new HashMap<>();
        map.put("code", success ? 0 : 1);
        map.put("msg", success ? "保存成功" : "保存失败");
        return map;
    }
}
