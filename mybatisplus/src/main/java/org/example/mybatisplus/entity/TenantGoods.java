package org.example.mybatisplus.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 多租户演示表：每个租户有自己的商品和库存（t_tenant_goods）。
 *
 * <p>注意实体里<b>没有</b> tenantId 字段：插入时由 TenantLineInnerInterceptor 自动填充，
 * 查询时自动拼 {@code WHERE tenant_id = 当前租户}，业务代码完全不用关心。
 */
@Data
@TableName("t_tenant_goods")
public class TenantGoods {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private BigDecimal price;

    private Integer stock;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
