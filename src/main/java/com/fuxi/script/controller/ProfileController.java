package com.fuxi.script.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fuxi.script.entity.SysUser;
import com.fuxi.script.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final SysUserService sysUserService;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/info")
    public String info(Model model) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        SysUser user = sysUserService.getOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        model.addAttribute("user", user);
        return "user/profile";
    }

    @PostMapping("/info")
    @ResponseBody
    public Map<String, Object> updateInfo(@RequestBody SysUser updatedUser) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        SysUser currentUser = sysUserService.getOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        
        // Only allow updating Real Name (and maybe other safe fields in future)
        currentUser.setRealName(updatedUser.getRealName());
        
        // Prevent password update here
        currentUser.setPassword(null);
        
        try {
            sysUserService.updateUser(currentUser);
            Map<String, Object> map = new HashMap<>();
            map.put("code", 0);
            map.put("msg", "更新成功");
            return map;
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> map = new HashMap<>();
            map.put("code", 1);
            map.put("msg", "更新失败: " + e.getMessage());
            return map;
        }
    }

    @GetMapping("/security")
    public String security() {
        return "user/security";
    }

    @PostMapping("/security")
    @ResponseBody
    public Map<String, Object> updateSecurity(@RequestBody Map<String, String> params) {
        String oldPassword = params.get("oldPassword");
        String newPassword = params.get("newPassword");
        String confirmPassword = params.get("confirmPassword");
        
        if (!StringUtils.hasText(oldPassword) || !StringUtils.hasText(newPassword)) {
            return error("密码不能为空");
        }
        
        if (!newPassword.equals(confirmPassword)) {
            return error("两次输入的新密码不一致");
        }
        
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        SysUser currentUser = sysUserService.getOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        
        if (!passwordEncoder.matches(oldPassword, currentUser.getPassword())) {
            return error("原密码错误");
        }
        
        currentUser.setPassword(newPassword);
        try {
            sysUserService.updateUser(currentUser);
            Map<String, Object> map = new HashMap<>();
            map.put("code", 0);
            map.put("msg", "密码修改成功");
            return map;
        } catch (Exception e) {
            e.printStackTrace();
            return error("修改失败: " + e.getMessage());
        }
    }
    
    private Map<String, Object> error(String msg) {
        Map<String, Object> map = new HashMap<>();
        map.put("code", 1);
        map.put("msg", msg);
        return map;
    }
}
