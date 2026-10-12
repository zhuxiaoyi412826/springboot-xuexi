package org.example.mvc.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class PhoneValidator implements ConstraintValidator<Phone, String> {

    private static final Pattern PHONE = Pattern.compile("^1[3-9]\\d{9}$");

    @Override
    public void initialize(Phone constraintAnnotation) {
        // 可读取注解上的属性做初始化
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            return true; // 为空交给 @NotBlank 处理
        }
        return PHONE.matcher(value).matches();
    }
}
