package com.fuxi.script.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fuxi.script.entity.SysUser;
import com.fuxi.script.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final SysUserMapper sysUserMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser sysUser = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username));
        if (sysUser == null) {
            throw new UsernameNotFoundException("User not found: " + username);
        }
        
        // Support multi-role (comma separated)
        java.util.List<org.springframework.security.core.GrantedAuthority> authorities = new java.util.ArrayList<>();
        if (sysUser.getRole() != null && !sysUser.getRole().isEmpty()) {
            String[] roles = sysUser.getRole().split(",");
            for (String role : roles) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role.trim()));
            }
        } else {
            // Fallback for empty role
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        }
        
        return new User(
                sysUser.getUsername(),
                sysUser.getPassword(),
                authorities
        );
    }
}
