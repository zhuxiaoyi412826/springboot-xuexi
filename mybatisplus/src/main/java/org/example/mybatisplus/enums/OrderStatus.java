package org.example.mybatisplus.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.IEnum;

/**
 * 订单状态枚举（MP 枚举处理）。
 *
 * <p>两种写法，这里同时演示：
 * <ul>
 *   <li>{@code @EnumValue} 标注「存进数据库的字段」——写库时存 code，读库时按 code 转枚举</li>
 *   <li>{@code implements IEnum<String>}——MP 提供 IEnum 接口，getValue() 的返回值即数据库值</li>
 * </ul>
 * 二选一即可，两个都写也不冲突。想要生效，必须配置
 * {@code mybatis-plus.configuration.default-enum-type-handler=com.baomidou.mybatisplus.core.handlers.MybatisEnumTypeHandler}
 * （已在 application.yaml 配好），否则 MyBatis 会默认按枚举名字（WAIT）存库。
 *
 * <p>JSON 序列化默认是枚举名（WAIT/SENT/DONE/CANCEL），前端用名字判断分支即可。
 */
public enum OrderStatus implements IEnum<String> {

    WAIT("待发货"),
    SENT("已发货"),
    DONE("已完成"),
    CANCEL("已取消");

    /** 真正存进数据库的值 */
    @EnumValue
    private final String code;

    OrderStatus(String code) {
        this.code = code;
    }

    /** IEnum 接口方法：数据库值 */
    @Override
    public String getValue() {
        return code;
    }

    public String getCode() {
        return code;
    }

    /** 按数据库值或枚举名解析，兼容前端传中文 / 传枚举名两种写法 */
    public static OrderStatus of(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String v = value.trim();
        for (OrderStatus status : values()) {
            if (status.code.equals(v) || status.name().equalsIgnoreCase(v)) {
                return status;
            }
        }
        return null;
    }
}
