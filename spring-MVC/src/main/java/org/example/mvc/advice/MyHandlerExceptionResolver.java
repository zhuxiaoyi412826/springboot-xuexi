package org.example.mvc.advice;

import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.mvc.common.Result;
import org.example.mvc.common.SpecialException;
import org.springframework.core.Ordered;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

import java.nio.charset.StandardCharsets;

/**
 * 自定义 HandlerExceptionResolver（异常解析器链的第一环）。
 *
 * <p>与 @ControllerAdvice 的区别：它更底层、先执行；返回非 null 的 ModelAndView 表示异常已处理，
 * 后续解析器（含 ExceptionHandlerExceptionResolver）不再执行。</p>
 *
 * <p>注册方式用 extendHandlerExceptionResolvers（追加），
 * 而不是 configureHandlerExceptionResolvers（会替换掉 Spring 默认的一整套解析器）。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MyHandlerExceptionResolver implements HandlerExceptionResolver, Ordered {

    private final ObjectMapper objectMapper;

    @Override
    public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response,
                                         Object handler, Exception ex) {
        if (!(ex instanceof SpecialException)) {
            return null; // 交给后续解析器
        }
        log.info("[MyHandlerExceptionResolver] 处理 SpecialException");
        try {
            response.setStatus(HttpServletResponse.SC_CONFLICT);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write(objectMapper.writeValueAsString(Result.fail(409, ex.getMessage())));
        } catch (Exception e) {
            log.error("写回响应失败", e);
        }
        return new ModelAndView(); // 空 ModelAndView 表示已处理
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
