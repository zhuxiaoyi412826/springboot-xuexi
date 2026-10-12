package org.example.mvc.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.example.mvc.common.Result;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.context.request.WebRequest;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Controller 返回值/视图 全家桶：
 * String 视图名、ModelAndView、forward、redirect、void、@ResponseBody、@ResponseStatus。
 */
@Slf4j
@Controller
@RequestMapping("/mvc/hello")
public class HelloController {

    /** 1. @ResponseBody：返回值交给 HttpMessageConverter 序列化 */
    @GetMapping(value = "/json", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public Result json() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("msg", "hello spring mvc");
        data.put("time", LocalDateTime.now().toString());
        return Result.ok(data);
    }

    /** 2. 返回普通字符串（同样走消息转换器） */
    @GetMapping(value = "/text", produces = "text/plain;charset=UTF-8")
    @ResponseBody
    public String text() {
        return "纯文本返回值（String + @ResponseBody）";
    }

    /** 3. 返回视图名：由自定义 StaticViewResolver 解析 static:hello -> /pages/hello.html（forward 渲染） */
    @GetMapping("/view")
    public String view(Model model) {
        model.addAttribute("title", "视图解析器演示");
        model.addAttribute("desc", "String 返回值 + Model，视图名 static:hello 被 StaticViewResolver 处理");
        return "static:hello";
    }

    /** 4. ModelAndView：模型与视图一起返回 */
    @GetMapping("/mav")
    public ModelAndView mav() {
        ModelAndView mav = new ModelAndView("static:hello");
        mav.addObject("title", "ModelAndView 演示");
        mav.addObject("desc", "ModelAndView 同时携带 model 与 view");
        return mav;
    }

    /** 5. forward：服务器内部转发，地址栏不变，request 共享 */
    @GetMapping("/forward")
    public String forward() {
        return "forward:/index.html";
    }

    /** 6. redirect：302 重定向，配合 RedirectAttributes 传参（Flash 属性存在 session 中，取一次即失效） */
    @GetMapping("/redirect")
    public String redirect(RedirectAttributes ra) {
        ra.addAttribute("from", "redirect");                 // -> URL 参数 ?from=redirect
        ra.addFlashAttribute("flashMsg", "这是一次性 Flash 消息"); // -> 存在 FlashMap
        return "redirect:/index.html";
    }

    /** 7. void：完全自己写响应（需要原生 HttpServletResponse） */
    @GetMapping("/void")
    public void voidReturn(HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":0,\"msg\":\"void 返回值：响应由自己写\"}");
    }

    /** 8. 原生 Servlet API 入参：HttpServletRequest / HttpSession / WebRequest */
    @GetMapping("/servlet-api")
    @ResponseBody
    public Result servletApi(HttpServletRequest request, HttpSession session, WebRequest webRequest) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("method", request.getMethod());
        data.put("uri", request.getRequestURI());
        data.put("remoteAddr", request.getRemoteAddr());
        data.put("sessionId", session.getId());
        data.put("webRequestHeader", webRequest.getHeader("User-Agent"));
        return Result.ok(data);
    }

    /** 9. @ResponseStatus：直接指定响应状态码 */
    @GetMapping("/created")
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public Result created() {
        return Result.ok("响应状态码被 @ResponseStatus 改成 201");
    }
}
