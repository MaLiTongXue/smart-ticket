package com.smartticket.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.smartticket.entity.SysUser;

import java.util.Map;

public interface SysUserService extends IService<SysUser> {

    /** 登录，成功返回 token 和用户信息 */
    Map<String, Object> login(String username, String password);

    /** 按用户名查用户 */
    SysUser getByUsername(String username);
}
