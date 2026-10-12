package org.example.mvc.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.mvc.common.Result;
import org.example.mvc.filter.RequestIdFilter;
import org.example.mvc.listener.MySessionListener;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 观察 Spring MVC 内部：HandlerMapping 里到底注册了哪些映射；监听器/过滤器留下的痕迹。
 */
@Slf4j
@RestController
@RequestMapping("/mvc/monitor")
@RequiredArgsConstructor
public class MonitorController {

    private final RequestMappingHandlerMapping handlerMapping;

    /** 所有 @RequestMapping 的注册信息（HandlerMapping 的核心数据） */
    @GetMapping("/mappings")
    public Result mappings() {
        Map<RequestMappingInfo, HandlerMethod> methods = handlerMapping.getHandlerMethods();
        List<Map<String, Object>> list = new ArrayList<>();
        methods.forEach((info, method) -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("pattern", String.valueOf(info));
            m.put("methods", String.valueOf(info.getMethodsCondition()));
            m.put("produces", String.valueOf(info.getProducesCondition()));
            m.put("handler", method.getBeanType().getSimpleName() + "#" + method.getMethod().getName());
            list.add(m);
        });
        list.sort((a, b) -> String.valueOf(a.get("pattern")).compareTo(String.valueOf(b.get("pattern"))));
        return Result.ok(Map.of("count", list.size(), "mappings", list));
    }

    /** 在线会话数（由 HttpSessionListener 统计） */
    @GetMapping("/sessions")
    public Result sessions() {
        return Result.ok(Map.of("online", MySessionListener.ONLINE.get()));
    }

    /** 过滤器写入的请求属性 */
    @GetMapping("/request-id")
    public Result requestId(@org.springframework.web.bind.annotation.RequestAttribute(RequestIdFilter.REQUEST_ID) String requestId) {
        return Result.ok(Map.of("requestId", requestId));
    }
}
