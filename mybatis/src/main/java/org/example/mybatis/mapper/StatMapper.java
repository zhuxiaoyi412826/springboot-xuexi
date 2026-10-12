package org.example.mybatis.mapper;

import org.apache.ibatis.annotations.Param;
import org.example.mybatis.dto.CategoryStatVO;

import java.util.List;
import java.util.Map;

/**
 * 统计报表 Mapper
 *
 * <p>所有 SQL 都写在 StatMapper.xml，集中演示：
 * <ul>
 *   <li>GROUP BY 分组 + HAVING 过滤</li>
 *   <li>聚合函数 COUNT / SUM / AVG / MAX / MIN / IFNULL / ROUND</li>
 *   <li>CASE WHEN 条件统计</li>
 *   <li>日期函数 DATE_FORMAT / DATE / YEAR</li>
 *   <li>UNION 合并结果</li>
 * </ul>
 */
public interface StatMapper {

    /** 各分类统计（COUNT/SUM/AVG/MAX/MIN + CASE WHEN） */
    List<CategoryStatVO> selectCategoryStat();

    /** 作者作品排行：只统计作品数 >= minCount 的作者（HAVING） */
    List<Map<String, Object>> selectAuthorRank(@Param("minCount") int minCount, @Param("limit") int limit);

    /** 总览：一个 Map 汇总全部核心指标 */
    Map<String, Object> selectOverview();

    /** 近 N 天每日新增小说数（日期分组统计） */
    List<Map<String, Object>> selectDailyNovelCount(@Param("days") int days);

    /** 近 N 天每日下载数 */
    List<Map<String, Object>> selectDailyDownloadCount(@Param("days") int days);

    /** 用户活跃排行：JOIN 多表统计 */
    List<Map<String, Object>> selectUserActiveRank(@Param("limit") int limit);
}
