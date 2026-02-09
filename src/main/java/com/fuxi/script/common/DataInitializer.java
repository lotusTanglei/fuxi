package com.fuxi.script.common;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fuxi.script.entity.SysSite;
import com.fuxi.script.entity.SysUser;
import com.fuxi.script.entity.SysUserSite;
import com.fuxi.script.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Order(100)
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final SysUserMapper sysUserMapper;
    private final SysSiteMapper sysSiteMapper;
    private final SysUserSiteMapper sysUserSiteMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        log.info("Initializing system data...");

        // 1. Initialize Default Site
        SysSite defaultSite = sysSiteMapper.selectOne(new LambdaQueryWrapper<SysSite>().eq(SysSite::getName, "总部"));
        if (defaultSite == null) {
            defaultSite = new SysSite();
            defaultSite.setName("总部");
            defaultSite.setDescription("Default Headquarters Site");
            sysSiteMapper.insert(defaultSite);
            log.info("Default Site created: 总部");
        }

        // 3. Initialize Admin User
        log.info("Checking for admin user...");
        SysUser admin = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, "admin"));
        if (admin == null) {
            log.info("Admin user not found. Creating default admin user.");
            admin = new SysUser();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("123456")); // Default password
            admin.setRealName("System Administrator");
            admin.setRole("ADMIN");
            
            sysUserMapper.insert(admin);
            
            // Add relations
            SysUserSite userSite = new SysUserSite();
            userSite.setUserId(admin.getId());
            userSite.setSiteId(defaultSite.getId());
            sysUserSiteMapper.insert(userSite);
            
            log.info("Default admin user created: username=admin, password=123456");
        } else {
            log.info("Admin user already exists. Role: {}", admin.getRole());
        }

        // 4. Initialize Dev User
        createDefaultUser("dev", "Developer", "DEV", defaultSite);
        
        // 5. Initialize Ops User
        createDefaultUser("ops", "Operator", "OPS", defaultSite);
        
        // 6. Initialize Leader User
        createDefaultUser("leader", "Team Leader", "LEADER", defaultSite);
        
        // 7. Initialize Test User
        createDefaultUser("test", "QA Engineer", "TEST", defaultSite);
    }

    private void createDefaultUser(String username, String realName, String role, SysSite defaultSite) {
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        if (user == null) {
            user = new SysUser();
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode("123456"));
            user.setRealName(realName);
            user.setRole(role);
            sysUserMapper.insert(user);
            
            SysUserSite userSite = new SysUserSite();
            userSite.setUserId(user.getId());
            userSite.setSiteId(defaultSite.getId());
            sysUserSiteMapper.insert(userSite);
            
            log.info("Default {} user created: username={}, password=123456", role, username);
        } else {
            log.info("User {} already exists. Role: {}", username, user.getRole());
        }
    }
}
