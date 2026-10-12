package org.example.mvc.resolver;

import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.example.mvc.annotation.WrapResult;
import org.example.mvc.common.Result;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.nio.charset.StandardCharsets;

/**
 * 自定义返回值处理器：把 {@code @WrapResult} 标注的方法返回值统一包装为 {@code Result} JSON。
 *
 * <p>注意：内置返回值处理器优先于自定义处理器，只有 catch-all（把返回值塞进 Model 的那个）
 * 排在自定义处理器之后。所以本 demo 的方法返回类型不能是 String/Map/ResponseEntity，
 * 否则会被内置处理器先处理。</p>
 */
@Component
public class WrapResultReturnValueHandler implements HandlerMethodReturnValueHandler {

    private final ObjectMapper objectMapper;

    public WrapResultReturnValueHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supportsReturnType(MethodParameter returnType) {
        return returnType.hasMethodAnnotation(WrapResult.class)
                || returnType.getContainingClass().isAnnotationPresent(WrapResult.class);
    }

    @Override
    public void handleReturnValue(Object returnValue,
                                  MethodParameter returnType,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest) throws Exception {
        mavContainer.setRequestHandled(true); // 表示响应已由本处理器直接写出，不再走视图渲染
        HttpServletResponse response = webRequest.getNativeResponse(HttpServletResponse.class);
        if (response == null) {
            return;
        }
        response.setStatus(200);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(Result.ok(returnValue)));
    }
}
