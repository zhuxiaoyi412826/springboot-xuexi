package org.example.mvc.controller;

import org.example.mvc.common.Result;
import org.springframework.context.MessageSource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.support.RequestContextUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 国际化：MessageSource（占位符/参数）、LocaleResolver、LocaleChangeInterceptor（?lang=en_US）。
 */
@RestController
@RequestMapping("/mvc/i18n")
public class I18nController {

    private final MessageSource messageSource;
    private final LocaleResolver localeResolver;

    public I18nController(MessageSource messageSource, LocaleResolver localeResolver) {
        this.messageSource = messageSource;
        this.localeResolver = localeResolver;
    }

    /** ?code=welcome */
    @GetMapping("/msg")
    public Result msg(@RequestParam(defaultValue = "welcome") String code, Locale locale) {
        return Result.ok(messageSource.getMessage(code, null, locale));
    }

    /** 带占位符：hello=你好，{0}！ */
    @GetMapping("/msg-args")
    public Result msgArgs(@RequestParam(defaultValue = "张三") String name, Locale locale) {
        return Result.ok(messageSource.getMessage("hello", new Object[]{name}, locale));
    }

    /** 当前 Locale（由 LocaleResolver 决定） */
    @GetMapping("/locale")
    public Result locale(HttpServletRequest request, Locale locale) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("locale", locale.toString());
        data.put("resolver", localeResolver.getClass().getSimpleName());
        data.put("fromRequestContext", String.valueOf(RequestContextUtils.getLocale(request)));
        return Result.ok(data);
    }

    /** 手动切换语言（等价于 LocaleChangeInterceptor 做的那件事） */
    @GetMapping("/change")
    public Result change(@RequestParam String lang,
                         HttpServletRequest request,
                         HttpServletResponse response) {
        localeResolver.setLocale(request, response, Locale.forLanguageTag(lang.replace('_', '-')));
        return Result.ok("语言已切换为 " + lang);
    }
}
