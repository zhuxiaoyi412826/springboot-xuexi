package org.example.mvc.validation;

import org.example.mvc.model.User;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

/**
 * Spring 自己的 {@code Validator} 接口（区别于 JSR-380），
 * 通过 {@code @InitBinder} 里的 {@code binder.setValidator(...)} 生效。
 */
public class UserValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return User.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "username", "required", "用户名必填（Spring Validator）");
        User user = (User) target;
        if (user.getAge() != null && user.getAge() < 18) {
            errors.rejectValue("age", "tooYoung", "未满 18 岁（Spring Validator）");
        }
        if (user.getNickname() != null && user.getNickname().contains("admin")) {
            errors.reject("nickname.invalid", "昵称不能包含 admin（全局错误，非字段错误）");
        }
    }
}
