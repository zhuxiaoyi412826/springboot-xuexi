package org.example.mybatisplus.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 自行车商品
 */
@Data
@TableName("t_bike")
public class Bike {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 商品名称 */
    private String name;

    /** 品牌 */
    private String brand;

    /** 分类：山地车 / 公路车 / 折叠车 / 城市车 / 儿童车 */
    private String category;

    /** 单价 */
    private BigDecimal price;

    /** 库存 */
    private Integer stock;

    /** 状态：1 在售，0 下架 */
    private Integer status;

    /** 图片地址 */
    private String imageUrl;

    /** 商品描述 */
    private String description;

    /** 自动填充：插入时回填（不再手写 LocalDateTime.now()） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 自动填充：插入 + 更新时回填 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除：默认 0。批量插入（insertBatchSomeColumn）不会帮我们填默认值，所以这里给初始值 */
    @TableLogic
    private Integer deleted = 0;

    /**
     * 乐观锁版本号（对应 OptimisticLockerInnerInterceptor）。
     * updateById 时会自动变成 {@code WHERE id = ? AND version = ?} 并 version + 1，
     * 更新行数为 0 就说明被别人抢先改了，需要重试或报错。
     */
    @Version
    private Integer version = 0;
}
