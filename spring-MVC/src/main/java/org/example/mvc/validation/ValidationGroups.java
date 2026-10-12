package org.example.mvc.validation;

/**
 * 校验分组：@Validated(Create.class) 只校验 Create 组的约束。
 */
public class ValidationGroups {

    public interface Create {
    }

    public interface Update {
    }
}
