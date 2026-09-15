package com.smartticket.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 实验用：观察 Filter 和 Interceptor 的执行顺序。
 *
 * 这是一个 Servlet 规范的过滤器（jakarta.servlet.Filter）。
 * 加了 @Component 之后，Spring Boot 会自动把它注册到 Servlet 容器里。
 *
 * 关键看输出顺序：
 *   >> [Filter] 进来   ← 最先
 *   ...
 *   << [Filter] 出去   ← 最后
 * 说明 Filter 把【整个请求】都包在里面了。
 */
@Component
public class MyLogFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        System.out.println(">>>>>>>>>> [Filter] 请求进来了 >>>>>>>>>>");

        // 放行，交给下一个环节（可能是下一个 Filter，也可能是 DispatcherServlet）
        chain.doFilter(request, response);

        System.out.println("<<<<<<<<<< [Filter] 请求出去了 <<<<<<<<<<");
    }
}
