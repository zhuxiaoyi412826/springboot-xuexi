package org.example.mvc.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.example.mvc.common.Result;
import org.example.mvc.model.User;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.NumberFormat;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.WebRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 参数绑定全家桶：@RequestParam / @PathVariable / @RequestHeader / @CookieValue /
 * @RequestAttribute / @ModelAttribute / POJO / 嵌套 / 集合 / 数组 / 原生 Servlet API。
 */
@Controller
@RequestMapping("/mvc/bind")
public class BindController {

    /** 方法级 @ModelAttribute：本 Controller 所有请求的 Model 都会带上 serverTime */
    @ModelAttribute("serverTime")
    public String serverTime() {
        return java.time.LocalDateTime.now().toString();
    }

    /** 1. 简单参数：required / defaultValue */
    @GetMapping("/simple")
    @ResponseBody
    public Result simple(@RequestParam("name") String name,
                         @RequestParam(value = "age", required = false, defaultValue = "18") int age,
                         @RequestParam(required = false) String[] hobbies) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("name", name);
        data.put("age", age);
        data.put("hobbies", hobbies == null ? List.of() : List.of(hobbies));
        return Result.ok(data);
    }

    /** 2. @RequestParam Map：把所有查询参数收进 Map */
    @GetMapping("/map")
    @ResponseBody
    public Result map(@RequestParam Map<String, String> params) {
        return Result.ok(params);
    }

    /** 3. @PathVariable：单个 + Map 全量 */
    @GetMapping("/path/{id}/{slug}")
    @ResponseBody
    public Result path(@PathVariable("id") Long id,
                       @PathVariable String slug,
                       @PathVariable Map<String, String> vars) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", id);
        data.put("slug", slug);
        data.put("all", vars);
        return Result.ok(data);
    }

    /** 4. 集合/数组：tags=a&tags=b 或 tags=a,b */
    @GetMapping("/list")
    @ResponseBody
    public Result list(@RequestParam List<String> tags, @RequestParam String[] ids) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("tags", tags);
        data.put("ids", List.of(ids));
        return Result.ok(data);
    }

    /**
     * 5. POJO 绑定（无注解时按「模型属性」绑定）：
     * /mvc/bind/pojo?username=zs&age=20&address.province=广东&address.city=深圳
     * &tags=a&tags=b&dept=10:研发部&birthday=2000-01-01&vipExpire=2030-12-31
     */
    @GetMapping("/pojo")
    @ResponseBody
    public Result pojo(User user) {
        return Result.ok(user);
    }

    /** 6. 日期与数字格式化（@DateTimeFormat / @NumberFormat） */
    @GetMapping("/date")
    @ResponseBody
    public Result date(@DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
                       @NumberFormat(pattern = "#,##0.00") BigDecimal amount) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("date", String.valueOf(date));
        data.put("amount", String.valueOf(amount));
        return Result.ok(data);
    }

    /** 7. 请求头：单个 + 全量 */
    @GetMapping("/header")
    @ResponseBody
    public Result header(@RequestHeader(value = "User-Agent", defaultValue = "unknown") String ua,
                         @RequestHeader Map<String, String> headers) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userAgent", ua);
        data.put("headerCount", headers.size());
        data.put("headers", headers);
        return Result.ok(data);
    }

    /** 8. Cookie */
    @GetMapping("/cookie")
    @ResponseBody
    public Result cookie(@CookieValue(value = "sid", defaultValue = "无 cookie") String sid) {
        return Result.ok(Map.of("sid", sid));
    }

    /** 9. @RequestAttribute：读取 Filter 放入 request 的属性 */
    @GetMapping("/attr")
    @ResponseBody
    public Result attr(@RequestAttribute("requestId") String requestId) {
        return Result.ok(Map.of("requestId", requestId));
    }

    /** 10. 原生 API：HttpServletRequest / Response / Session / WebRequest / NativeWebRequest */
    @GetMapping("/servlet-api")
    @ResponseBody
    public Result servletApi(HttpServletRequest request,
                             HttpServletResponse response,
                             HttpSession session,
                             WebRequest webRequest,
                             NativeWebRequest nativeWebRequest) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("queryString", request.getQueryString());
        data.put("sessionNew", session.isNew());
        data.put("parameterNames", request.getParameterMap().keySet());
        data.put("webRequest", webRequest.getDescription(false));
        data.put("nativeResponse", nativeWebRequest.getNativeResponse(HttpServletResponse.class) != null);
        return Result.ok(data);
    }

    /** 11. @ModelAttribute + BindingResult：手工处理绑定/校验错误（BindingResult 必须紧跟在被校验参数后） */
    @PostMapping("/model-attr")
    @ResponseBody
    public Result modelAttr(@ModelAttribute("u") User user, BindingResult br) {
        if (br.hasErrors()) {
            Map<String, String> errors = new LinkedHashMap<>();
            br.getFieldErrors().forEach(fe -> errors.put(fe.getField(), fe.getDefaultMessage()));
            return Result.fail(400, "绑定/校验失败：" + errors);
        }
        return Result.ok(user);
    }

    /**
     * 12. @InitBinder：只对名为 "u" 的模型属性生效。
     * setAllowedFields 是安全白名单（防止恶意提交多余字段，如 id/role）。
     */
    @InitBinder("u")
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields("username", "nickname", "age", "phone", "birthday",
                "vipExpire", "address.province", "address.city", "address.detail", "tags", "dept");
        binder.setDisallowedFields("id");
    }
}
