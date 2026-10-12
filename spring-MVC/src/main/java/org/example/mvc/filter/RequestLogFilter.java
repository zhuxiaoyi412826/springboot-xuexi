package org.example.mvc.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Filter：用 ContentCachingRequestWrapper 缓存请求体，解决「请求体只能读一次」的问题，
 * 并打印耗时与状态码（Filter 能拿到最原始的请求/响应）。
 */
@Slf4j
public class RequestLogFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long start = System.currentTimeMillis();
        // Spring 6+ 需要显式指定缓存上限（字节）
        ContentCachingRequestWrapper wrapper = new ContentCachingRequestWrapper(request, 1024 * 8);
        try {
            chain.doFilter(wrapper, response);
        } finally {
            byte[] buf = wrapper.getContentAsByteArray();
            String body = new String(buf, StandardCharsets.UTF_8);
            if (StringUtils.hasText(body) && body.length() > 200) {
                body = body.substring(0, 200) + "...(截断)";
            }
            log.info("[RequestLogFilter] {} {} cost={}ms status={} body={}",
                    wrapper.getMethod(), wrapper.getRequestURI(),
                    System.currentTimeMillis() - start, response.getStatus(), body);
        }
    }
}
