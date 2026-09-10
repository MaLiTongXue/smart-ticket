package com.smartticket.controller;

import com.smartticket.common.Result;
import com.smartticket.dto.LoginRequest;
import com.smartticket.service.SysUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 认证接口。这个类的 /api/auth/login 在 WebConfig 里被放行了，不需要登录就能访问。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final SysUserService sysUserService;

    public AuthController(SysUserService sysUserService) {
        this.sysUserService = sysUserService;
    }

    /**
     * 登录
     * POST /api/auth/login
     * {"username":"admin","password":"123456"}
     * 返回 token，前端存起来，之后每个请求都放在请求头里
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(sysUserService.login(request.getUsername(), request.getPassword()));
    }

    /**
     * 获取当前登录用户信息。
     * userId 是拦截器验证 token 之后塞进 request 的，所以这里直接取就行。
     */
    @GetMapping("/info")
    public Result<Map<String, Object>> info(HttpServletRequest request) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("userId", request.getAttribute("userId"));
        map.put("username", request.getAttribute("username"));
        return Result.success(map);
    }
}
