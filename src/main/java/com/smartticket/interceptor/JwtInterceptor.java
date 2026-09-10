package com.smartticket.interceptor;

import com.smartticket.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录拦截器。
 * 请求进来时先检查请求头里有没有带合法的 token，没带就直接拦掉，不让进 Controller。
 *
 * 前端调用时要在请求头里加：Authorization: Bearer <token>
 */
@Component
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    public JwtInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        // 浏览器跨域时会先发一个 OPTIONS 预检请求，这个必须放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = request.getHeader("Authorization");
        // 前端习惯写成 "Bearer xxx"，把前缀去掉
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        if (token == null || token.isEmpty()) {
            writeError(response, "未登录，请先登录");
            return false;
        }

        try {
            Claims claims = jwtUtil.parseToken(token);
            // 把用户信息塞进 request，Controller 里用 request.getAttribute("userId") 就能取到
            request.setAttribute("userId", Long.valueOf(claims.getSubject()));
            request.setAttribute("username", claims.get("username"));
            return true;
        } catch (Exception e) {
            // 签名不对或者已过期
            writeError(response, "登录已过期，请重新登录");
            return false;
        }
    }

    /** 拦截时直接写 JSON 回去。注意 HTTP 状态码用 200，把错误码放在 body 的 code 里，前端处理更简单 */
    private void writeError(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"message\":\"" + message + "\",\"data\":null}");
    }
}
