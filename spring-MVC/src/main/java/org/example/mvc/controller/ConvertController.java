package org.example.mvc.controller;

import org.example.mvc.common.Result;
import org.example.mvc.model.Dept;
import org.springframework.format.annotation.NumberFormat;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 类型转换与格式化：自定义 Converter / Formatter / ConversionService / @NumberFormat。
 */
@Controller
@RequestMapping("/mvc/convert")
public class ConvertController {

    /** 自定义 Converter：dept=10:研发部 -> Dept 对象 */
    @GetMapping("/dept")
    @ResponseBody
    public Result dept(@RequestParam Dept dept) {
        return Result.ok(dept);
    }

    /** 自定义 Formatter：money=1,234.56 -> BigDecimal */
    @GetMapping("/money")
    @ResponseBody
    public Result money(@RequestParam BigDecimal money) {
        return Result.ok(Map.of("money", money.toString()));
    }

    /** @NumberFormat 注解：amount=1234.567 按 #,##0.00 解析 */
    @GetMapping("/number")
    @ResponseBody
    public Result number(@NumberFormat(pattern = "#,##0.00") @RequestParam BigDecimal amount) {
        return Result.ok(Map.of("amount", amount.toString()));
    }

    /** 编程式使用 ConversionService（与 Spring MVC 用的是同一套机制） */
    @GetMapping("/service")
    @ResponseBody
    public Result service() {
        FormattingConversionService cs = new DefaultFormattingConversionService();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("stringToInteger", cs.convert("123", Integer.class));
        data.put("stringToBoolean", cs.convert("true", Boolean.class));
        data.put("stringToEnum", cs.convert("GET", RequestMethodEnum.class));
        data.put("stringToLocalDate", cs.convert("2026-01-01", java.time.LocalDate.class).toString());
        data.put("canConvert(String->Dept)", cs.canConvert(String.class, Dept.class)
                ? "false（Dept 转换器注册在 MVC 的 ConversionService 里，不在本示例新建的这个里）" : "false");
        return Result.ok(data);
    }

    public enum RequestMethodEnum {
        GET, POST
    }
}
