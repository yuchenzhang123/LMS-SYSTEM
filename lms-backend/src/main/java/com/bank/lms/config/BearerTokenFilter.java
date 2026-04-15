package com.bank.lms.config;

import com.alibaba.fastjson.JSON;
import com.bank.lms.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Bearer Token 校验过滤器
 * 从 Authorization 头取 token，调 OAuth check_token 端点验证有效性
 * auth.enabled=false 时（开发环境）直接放行
 */
@Slf4j
@Component
public class BearerTokenFilter extends OncePerRequestFilter {

    @Value("${auth.enabled:true}")
    private boolean authEnabled;

    @Value("${auth.token-check-url:}")
    private String tokenCheckUrl;

    // 不需要校验的路径前缀
    private static final String[] WHITELIST = {
        "/swagger", "/v2/api-docs", "/webjars", "/favicon.ico",
        "/admin/scheduler"
    };

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (!authEnabled) {
            chain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        for (String prefix : WHITELIST) {
            if (path.startsWith(prefix)) {
                chain.doFilter(request, response);
                return;
            }
        }

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            writeUnauthorized(response, "缺少 Authorization 令牌");
            return;
        }

        String token = header.substring(7).trim();
        if (!validateToken(token)) {
            writeUnauthorized(response, "令牌无效或已过期");
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean validateToken(String token) {
        if (tokenCheckUrl == null || tokenCheckUrl.trim().isEmpty()) {
            log.warn("auth.token-check-url 未配置，跳过 token 校验");
            return true;
        }
        try {
            String urlStr = tokenCheckUrl + "?token=" + token;
            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            int status = conn.getResponseCode();
            if (status == 200) {
                return true;
            }
            log.warn("Token 校验失败，HTTP 状态码: {}", status);
            return false;
        } catch (Exception e) {
            log.error("Token 校验请求异常: {}", e.getMessage());
            return false;
        }
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(JSON.toJSONString(Result.error("1002", message)));
    }
}
