package org.example.mybatis.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 分类统计 VO（GROUP BY 聚合 + SQL 函数）
 */
@Data
public class CategoryStatVO {

    private Long categoryId;
    private String categoryName;

    /** 小说数 COUNT */
    private Long novelCount;

    /** 总字数 SUM */
    private Long totalWords;

    /** 平均字数 AVG */
    private BigDecimal avgWords;

    /** 最大/最小字数 MAX MIN */
    private Long maxWords;
    private Long minWords;

    /** 总浏览量 SUM */
    private Long totalViews;

    /** 平均浏览量 AVG */
    private BigDecimal avgViews;

    /** 完结作品数 SUM(CASE WHEN ...) */
    private Long finishedCount;

    /** 收藏总数 SUM */
    private Long totalFavorites;
}
