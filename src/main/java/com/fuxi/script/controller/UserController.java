package com.fuxi.script.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fuxi.script.entity.SysSite;
import com.fuxi.script.entity.SysUser;
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
import java.util.stream.Collectors;

@Controller
@RequestMapping("/user")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final SysUserService sysUserService;
    private final SysSiteService sysSiteService;

    @GetMapping("/list")
    public String list() {
        return "user/list";
    }

    @GetMapping("/api/leaders")
    @ResponseBody
    @PreAuthorize("hasAnyRole('ADMIN', 'DEV', 'LEADER')") // Allow DEV/LEADER to see list of leaders
    public Map<String, Object> getLeaders() {
        List<SysUser> leaders = sysUserService.list(new LambdaQueryWrapper<SysUser>()
                .like(SysUser::getRole, "LEADER"));
        
        // Hide sensitive info
        leaders.forEach(u -> u.setPassword(null));
        
        Map<String, Object> map = new HashMap<>();
        map.put("code", 0);
        map.put("data", leaders);
        return map;
    }

    @GetMapping("/api/list")
    @ResponseBody
    public Map<String, Object> apiList(@RequestParam(defaultValue = "1") Integer page,
                                       @RequestParam(defaultValue = "10") Integer limit,
                                       @RequestParam(required = false) String username) {
        Page<SysUser> pageParam = new Page<>(page, limit);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(username)) {
            wrapper.like(SysUser::getUsername, username);
        }
        wrapper.orderByDesc(SysUser::getId);
        
        Page<SysUser> result = sysUserService.page(pageParam, wrapper);
        
        // Removed site/group name filling logic as relation is now N:N
        
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
            SysUser user = sysUserService.getById(id);
            // Hide password
            user.setPassword(null);
            model.addAttribute("user", user);
        } else {
            model.addAttribute("user", new SysUser());
        }
        
        // Add sites for dropdowns
        model.addAttribute("sites", sysSiteService.list());
        
        return "user/form";
    }

    @PostMapping("/save")
    @ResponseBody
    public Map<String, Object> save(@RequestBody SysUser user) {
        System.out.println("Saving user: " + user);
        try {
            boolean success;
            if (user.getId() != null) {
                success = sysUserService.updateUser(user);
            } else {
                success = sysUserService.createUser(user);
            }
            
            Map<String, Object> map = new HashMap<>();
            map.put("code", success ? 0 : 1);
            map.put("msg", success ? "保存成功" : "保存失败");
            return map;
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> map = new HashMap<>();
            map.put("code", 1);
            map.put("msg", "Error: " + e.getMessage());
            return map;
        }
    }
    
    @PostMapping("/delete/{id}")
    @ResponseBody
    public Map<String, Object> delete(@PathVariable Long id) {
        boolean success = sysUserService.removeById(id);
        Map<String, Object> map = new HashMap<>();
        map.put("code", success ? 0 : 1);
        map.put("msg", success ? "删除成功" : "删除失败");
        return map;
    }
}