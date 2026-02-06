package com.fuxi.script.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.fuxi.script.entity.SysUser;

public interface SysUserService extends IService<SysUser> {
    
    /**
     * Create user with password encryption
     */
    boolean createUser(SysUser user);

    /**
     * Update user (handle password update if present)
     */
    boolean updateUser(SysUser user);

    /**
     * Get user IDs by site ID
     */
    java.util.List<Long> getUserIdsBySiteId(Long siteId);

    /**
     * Configure users for a site
     */
    boolean configSiteUsers(Long siteId, java.util.List<Long> userIds);
}
