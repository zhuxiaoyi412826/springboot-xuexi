package org.example.mvc.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.example.mvc.validation.Phone;
import org.example.mvc.validation.ValidationGroups;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

/**
 * 用户模型，集中演示：
 * <ul>
 *     <li>JSR-380 校验注解 + 校验分组（Create / Update）</li>
 *     <li>自定义校验注解 {@code @Phone}</li>
 *     <li>嵌套对象绑定（address）、List 绑定（tags）</li>
 *     <li>{@code @DateTimeFormat} 表单日期绑定 / {@code @JsonFormat} JSON 日期格式化</li>
 *     <li>{@code @JsonView} 按视图裁剪 JSON 字段</li>
 *     <li>自定义类型 Converter（dept：字符串 "10:研发部"）</li>
 * </ul>
 */
@Data
public class User implements Serializable {

    /** JSON 视图：Public 为基础视图，Admin 继承 Public（多返回敏感字段） */
    public interface Views {
        interface Public {
        }

        interface Admin extends Public {
        }
    }

    @JsonView(Views.Public.class)
    @NotNull(message = "id 不能为空", groups = ValidationGroups.Update.class)
    private Long id;

    @JsonView(Views.Public.class)
    @NotBlank(message = "用户名不能为空", groups = {ValidationGroups.Create.class, ValidationGroups.Update.class})
    @Size(min = 2, max = 20, message = "用户名长度必须在 2~20 之间")
    private String username;

    @JsonView(Views.Public.class)
    private String nickname;

    @JsonView(Views.Public.class)
    @Min(value = 1, message = "年龄最小为 1")
    @Max(value = 120, message = "年龄最大为 120")
    private Integer age;

    /** 自定义校验注解 */
    @JsonView(Views.Admin.class)
    @Phone
    private String phone;

    @JsonView(Views.Admin.class)
    private BigDecimal balance;

    /** 表单绑定时按该格式解析：/mvc/bind/pojo?birthday=2000-01-01 */
    @org.springframework.format.annotation.DateTimeFormat(pattern = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthday;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** java.util.Date：由全局 @InitBinder 注册的自定义 PropertyEditor 解析（指定时区，避免序列化差一天） */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Shanghai")
    private Date vipExpire;

    /** 嵌套对象：/mvc/bind/pojo?address.province=广东&address.city=深圳 */
    @Valid
    private Address address;

    /** 集合绑定：/mvc/bind/pojo?tags=a&tags=b 或 tags=a,b */
    private List<String> tags;

    /** 自定义 Converter：/mvc/bind/pojo?dept=10:研发部 */
    private Dept dept;
}
