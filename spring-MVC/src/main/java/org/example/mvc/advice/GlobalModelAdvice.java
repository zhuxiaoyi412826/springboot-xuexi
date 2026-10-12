package org.example.mvc.advice;

import org.example.mvc.convert.DatePropertyEditor;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Date;

/**
 * 全局增强：
 * <ul>
 *     <li>{@code @ModelAttribute} 方法：给所有 Controller 的 Model 注入公共数据</li>
 *     <li>{@code @InitBinder} 方法：注册全局自定义属性编辑器</li>
 * </ul>
 */
@ControllerAdvice(basePackages = "org.example.mvc.controller")
public class GlobalModelAdvice {

    @ModelAttribute("siteName")
    public String siteName() {
        return "Spring MVC 实验室";
    }

    /** 全局注册：所有 Controller 里出现 java.util.Date 的绑定都走这个编辑器 */
    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(Date.class, new DatePropertyEditor());
    }
}
