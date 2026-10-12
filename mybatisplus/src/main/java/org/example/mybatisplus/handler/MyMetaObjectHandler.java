package org.example.mybatisplus.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 自动填充：create_time / update_time 由 MP 统一回填，业务代码不再手写 LocalDateTime.now()。
 *
 * <p>生效条件：实体字段上标了 {@code @TableField(fill = FieldFill.INSERT / INSERT_UPDATE)}。
 * strictXxxFill 的「strict」表示严格模式：字段不存在或已有值时不覆盖，避免误填充。
 */
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }
}
