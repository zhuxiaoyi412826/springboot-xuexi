package org.example.mybatisplus.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品高级检索条件（动态 SQL 的入参）
 *
 * <p>所有字段都可以为空，Mapper 里用 &lt;if&gt; 判断后再拼接，做到"传什么条件拼什么条件"。
 */
@Data
public class BikeQuery {

    /** 关键字：商品名 / 品牌 / 描述 模糊匹配 */
    private String keyword;

    /** 分类多选，如 ["山地车","公路车"] */
    private List<String> categories;

    /** 品牌多选 */
    private List<String> brands;

    /** 价格区间下限 */
    private BigDecimal minPrice;

    /** 价格区间上限 */
    private BigDecimal maxPrice;

    /** 最低库存（<=0 表示不限） */
    private Integer minStock;

    /** 商品状态：1 在售 / 0 下架 / null 全部 */
    private Integer status;

    /** 最低销量（需要关联订单表统计） */
    private Integer minSold;

    /** 排序：price_asc / price_desc / stock_asc / stock_desc / sold_desc / id_desc */
    private String orderBy;
}
