package org.example.mybatisplus.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 分类销售统计结果（GROUP BY 聚合）
 */
@Data
public class CategoryStatVO {

    /** 分类 */
    private String category;

    /** 商品款数 */
    private Long bikeCount;

    /** 库存合计 */
    private Long totalStock;

    /** 平均价 */
    private BigDecimal avgPrice;

    /** 最高价 */
    private BigDecimal maxPrice;

    /** 最低价 */
    private BigDecimal minPrice;

    /** 销量合计 */
    private Long soldCount;

    /** 销售额合计 */
    private BigDecimal salesAmount;
}
