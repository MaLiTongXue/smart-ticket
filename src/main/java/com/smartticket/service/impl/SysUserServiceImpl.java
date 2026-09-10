package com.smartticket.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.smartticket.common.BizException;
import com.smartticket.entity.SysUser;
import com.smartticket.mapper.SysUserMapper;
import com.smartticket.service.SysUserService;
import com.smartticket.util.JwtUtil;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    private final JwtUtil jwtUtil;

    public SysUserServiceImpl(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public SysUser getByUsername(String username) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, username);
        return this.getOne(wrapper);
    }

    @Override
    public Map<String, Object> login(String username, String password) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw new BizException("用户名和密码不能为空");
        }

        SysUser user = getByUsername(username);

        // 安全考虑：用户不存在和密码错误，返回同一句提示，避免被人枚举出哪些用户名存在
        if (user == null) {
            throw new BizException("用户名或密码错误");
        }

        // 把前端传来的明文密码做 MD5，再和数据库里存的比对
        String inputMd5 = DigestUtils.md5DigestAsHex(password.getBytes(StandardCharsets.UTF_8));
        if (!inputMd5.equals(user.getPassword())) {
            throw new BizException("用户名或密码错误");
        }

        // 生成 token
        String token = jwtUtil.createToken(user.getId(), user.getUsername());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("token", token);
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("nickname", user.getNickname());
        result.put("role", user.getRole());
        return result;
    }
}
