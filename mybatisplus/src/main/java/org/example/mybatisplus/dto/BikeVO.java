package org.example.mybatisplus.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品检索结果（比 Bike 多了关联订单统计出来的销量与销售额）
 */
@Data
public class BikeVO {

    private Long id;
    private String name;
    private String brand;
    private String category;
    private BigDecimal price;
    private Integer stock;
    private Integer status;
    private String imageUrl;
    private String description;

    /** 累计销量（不含已取消订单） */
    private Long soldCount;

    /** 累计销售额（不含已取消订单） */
    private BigDecimal salesAmount;
}
