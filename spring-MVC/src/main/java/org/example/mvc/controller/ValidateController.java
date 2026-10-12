package org.example.mvc.controller;

import jakarta.validation.Valid;
import org.example.mvc.common.Result;
import org.example.mvc.model.User;
import org.example.mvc.validation.UserValidator;
import org.example.mvc.validation.ValidationGroups;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 数据校验：
 * <ul>
 *     <li>{@code @Valid @RequestBody}：失败抛 MethodArgumentNotValidException，交给全局异常处理</li>
 *     <li>{@code @Valid + BindingResult}：失败不抛异常，自己在方法里处理</li>
 *     <li>{@code @Validated(分组)}：只校验指定分组的约束</li>
 *     <li>{@code @InitBinder + binder.setValidator}：使用 Spring 自己的 Validator 接口</li>
 * </ul>
 */
@Controller
@RequestMapping("/mvc/validate")
public class ValidateController {

    /** 1. @RequestBody + @Valid：抛异常，由 GlobalExceptionHandler 转成 400 */
    @PostMapping("/body")
    @ResponseBody
    public Result body(@Valid @RequestBody User user) {
        return Result.ok(user);
    }

    /** 2. 表单 + @Valid + BindingResult：方法内自己处理错误 */
    @GetMapping("/form")
    @ResponseBody
    public Result form(@Valid @ModelAttribute("v") User user, BindingResult br) {
        if (br.hasErrors()) {
            Map<String, String> errors = new LinkedHashMap<>();
            for (FieldError fe : br.getFieldErrors()) {
                errors.put(fe.getField(), fe.getDefaultMessage());
            }
            br.getGlobalErrors().forEach(oe -> errors.put("global", oe.getDefaultMessage()));
            return Result.fail(400, "校验失败：" + errors);
        }
        return Result.ok(user);
    }

    /** 3. 校验分组：Create 组只校验 Create 组的约束（id 不校验，username 校验） */
    @PostMapping("/create")
    @ResponseBody
    public Result create(@Validated(ValidationGroups.Create.class) @RequestBody User user) {
        return Result.ok(user);
    }

    /** 4. 校验分组：Update 组要求 id 必填 */
    @PostMapping("/update")
    @ResponseBody
    public Result update(@Validated(ValidationGroups.Update.class) @RequestBody User user) {
        return Result.ok(user);
    }

    /** 5. Spring 原生 Validator：见 @InitBinder("sv") */
    @GetMapping("/spring-validator")
    @ResponseBody
    public Result springValidator(@Valid @ModelAttribute("sv") User user, BindingResult br) {
        if (br.hasErrors()) {
            Map<String, String> errors = new LinkedHashMap<>();
            br.getFieldErrors().forEach(fe -> errors.put(fe.getField(), fe.getDefaultMessage()));
            br.getGlobalErrors().forEach(oe -> errors.put(oe.getObjectName(), oe.getDefaultMessage()));
            return Result.fail(400, "Spring Validator 报错：" + errors);
        }
        return Result.ok(user);
    }

    /** 给名为 "sv" 的模型属性换成自定义的 Spring Validator（替换默认的 JSR-380 校验器） */
    @InitBinder("sv")
    public void initValidator(WebDataBinder binder) {
        binder.setValidator(new UserValidator());
    }
}
