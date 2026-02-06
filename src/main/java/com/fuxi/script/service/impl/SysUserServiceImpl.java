package com.fuxi.script.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fuxi.script.entity.SysUser;
import com.fuxi.script.entity.SysUserSite;
import com.fuxi.script.mapper.SysUserMapper;
import com.fuxi.script.mapper.SysUserSiteMapper;
import com.fuxi.script.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    private final PasswordEncoder passwordEncoder;
    private final SysUserSiteMapper sysUserSiteMapper;

    @Override
    public boolean createUser(SysUser user) {
        if (StringUtils.hasText(user.getPassword())) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        } else {
            // Default password if not provided
            user.setPassword(passwordEncoder.encode("123456"));
        }
        return this.save(user);
    }

    @Override
    public boolean updateUser(SysUser user) {
        if (StringUtils.hasText(user.getPassword())) {
            // If password field is not empty, update password
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        } else {
            // If password field is empty, do not update password
            user.setPassword(null);
        }
        return this.updateById(user);
    }

    @Override
    public List<Long> getUserIdsBySiteId(Long siteId) {
        return sysUserSiteMapper.selectList(new LambdaQueryWrapper<SysUserSite>()
                .eq(SysUserSite::getSiteId, siteId))
                .stream()
                .map(SysUserSite::getUserId)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean configSiteUsers(Long siteId, List<Long> userIds) {
        // Delete existing relations
        sysUserSiteMapper.delete(new LambdaQueryWrapper<SysUserSite>().eq(SysUserSite::getSiteId, siteId));
        
        // Insert new relations
        if (userIds != null && !userIds.isEmpty()) {
            for (Long userId : userIds) {
                SysUserSite relation = new SysUserSite();
                relation.setSiteId(siteId);
                relation.setUserId(userId);
                sysUserSiteMapper.insert(relation);
            }
        }
        return true;
    }
}
