package org.example.mvc.convert;

import org.example.mvc.model.Dept;
import org.springframework.core.convert.converter.Converter;
import org.springframework.util.StringUtils;

/**
 * 自定义类型转换器：String -> Dept（"10:研发部"）。
 * 通过 {@code WebMvcConfigurer#addFormatters} 注册进 ConversionService，
 * 之后所有 Controller 参数 / @ModelAttribute 绑定中出现的 Dept 都会走这里。
 */
public class StringToDeptConverter implements Converter<String, Dept> {

    @Override
    public Dept convert(String source) {
        if (!StringUtils.hasText(source)) {
            return null;
        }
        String[] arr = source.split(":");
        Dept dept = new Dept();
        dept.setId(Long.valueOf(arr[0].trim()));
        dept.setName(arr.length > 1 ? arr[1].trim() : "未知部门");
        return dept;
    }
}
