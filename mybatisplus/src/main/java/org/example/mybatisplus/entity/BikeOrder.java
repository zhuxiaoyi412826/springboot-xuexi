package org.example.mybatisplus.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.example.mybatisplus.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单（表名 t_order，ORDER 是 SQL 关键字，所以用 t_order）
 *
 * <p>status 使用了 MP 的枚举处理：Java 里是 {@link OrderStatus} 枚举，数据库里存中文（待发货/已发货/...）。
 */
@Data
@TableName("t_order")
public class BikeOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单编号 */
    private String orderNo;

    private Long userId;

    /** 下单用户名（冗余） */
    private String username;

    private Long bikeId;

    /** 商品名（冗余） */
    private String bikeName;

    /** 下单时单价快照 */
    private BigDecimal price;

    /** 购买数量 */
    private Integer quantity;

    /** 总金额 */
    private BigDecimal totalAmount;

    /** 收货人 */
    private String receiver;

    /** 联系电话 */
    private String phone;

    /** 收货地址 */
    private String address;

    /** 订单状态：枚举，数据库存 getValue()（中文） */
    private OrderStatus status;

    /** 自动填充：插入时回填 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 自动填充：插入 + 更新时回填 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;

    /** 状态中文名，方便前端直接展示 */
    public String getStatusText() {
        return status == null ? null : status.getCode();
    }
}
