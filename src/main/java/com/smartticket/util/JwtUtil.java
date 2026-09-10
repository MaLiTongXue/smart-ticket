package com.smartticket.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类。
 *
 * JWT 是什么（面试必问）：它就是一段字符串，格式是 头部.载荷.签名。
 *   - 头部：说明用什么算法签名
 *   - 载荷：存用户 id、用户名、过期时间（注意：这部分只是 Base64 编码，不是加密，别放密码！）
 *   - 签名：用密钥对前两部分做 HMAC，防止别人篡改
 *
 * 和 Session 的区别：Session 存在服务器内存里，JWT 存在客户端，服务器不用存，
 * 所以天生适合分布式部署（多台服务器都能验签，不用共享 Session）。
 */
@Component
public class JwtUtil {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expire}")
    private Long expire;

    /** 由密钥字符串生成签名用的 Key */
    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /** 生成 token */
    public String createToken(Long userId, String username) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))          // 载荷里放用户 id
                .claim("username", username)              // 自定义字段
                .issuedAt(now)                            // 签发时间
                .expiration(new Date(now.getTime() + expire))  // 过期时间
                .signWith(getKey())                       // 签名
                .compact();
    }

    /** 解析 token。如果 token 被改过或者过期了，这里会抛异常 */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
