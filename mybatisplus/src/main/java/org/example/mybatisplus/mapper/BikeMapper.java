package org.example.mybatisplus.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.mybatisplus.dto.BikeQuery;
import org.example.mybatisplus.dto.BikeVO;
import org.example.mybatisplus.dto.CategoryStatVO;
import org.example.mybatisplus.entity.Bike;

import java.util.List;

public interface BikeMapper extends EasyBaseMapper<Bike> {

    // ==================================================================
    // 悲观锁：SELECT ... FOR UPDATE
    // 必须在事务内执行（@Transactional），否则 MySQL 会自动提交，锁立即释放、等于没加锁
    // 作用：事务期间独占该行（排他锁），其它事务的 FOR UPDATE / UPDATE 会被阻塞，直到本事务提交或回滚
    // ==================================================================

    /**
     * 按 id 查询并加行级排他锁（FOR UPDATE）
     */
    @Select("SELECT * FROM t_bike WHERE id = #{id} AND deleted = 0 FOR UPDATE")
    Bike selectByIdForUpdate(@Param("id") Long id);

    // ==================================================================
    // 方式一：Mapper XML —— SQL 写在 resources/mapper/BikeMapper.xml
    // 优点：SQL 与 Java 解耦、支持 <sql> 片段复用、复杂 SQL 可读性好、可热改
    // ==================================================================

    /**
     * 动态条件分页检索商品（XML 实现），销量通过 LEFT JOIN 订单子查询得出
     */
    IPage<BikeVO> searchWithXml(Page<?> page, @Param("q") BikeQuery query);

    /**
     * 动态条件 + 分组聚合统计（XML 实现）
     */
    List<CategoryStatVO> statWithXml(@Param("q") BikeQuery query);

    // ==================================================================
    // 方式二：接口注解 —— SQL 直接写在 @Select("<script>...</script>") 里
    // 优点：不用额外 XML 文件，SQL 与方法定义在一起，短 SQL 很方便
    // 注意：注解里同样支持 <if> <where> <foreach> <choose> 等全部动态标签
    // ==================================================================

    /**
     * 与 searchWithXml 完全等价的注解版动态 SQL（分页）
     */
    @Select({
            "<script>",
            "SELECT b.id, b.name, b.brand, b.category, b.price, b.stock, b.status, b.image_url, b.description,",
            "       COALESCE(so.sold_count, 0)    AS sold_count,",
            "       COALESCE(so.sales_amount, 0)  AS sales_amount",
            "FROM t_bike b",
            "LEFT JOIN (",
            "    SELECT bike_id,",
            "           SUM(quantity)     AS sold_count,",
            "           SUM(total_amount) AS sales_amount",
            "    FROM t_order",
            "    WHERE deleted = 0 AND status &lt;&gt; '已取消'",
            "    GROUP BY bike_id",
            ") so ON so.bike_id = b.id",
            "<where>",
            "    AND b.deleted = 0",
            "    <if test='q.keyword != null and q.keyword != &quot;&quot;'>",
            "        AND (b.name LIKE CONCAT('%', #{q.keyword}, '%')",
            "          OR b.brand LIKE CONCAT('%', #{q.keyword}, '%')",
            "          OR b.description LIKE CONCAT('%', #{q.keyword}, '%'))",
            "    </if>",
            "    <if test='q.categories != null and q.categories.size() &gt; 0'>",
            "        AND b.category IN",
            "        <foreach collection='q.categories' item='c' open='(' separator=',' close=')'>#{c}</foreach>",
            "    </if>",
            "    <if test='q.brands != null and q.brands.size() &gt; 0'>",
            "        AND b.brand IN",
            "        <foreach collection='q.brands' item='b2' open='(' separator=',' close=')'>#{b2}</foreach>",
            "    </if>",
            "    <if test='q.minPrice != null'>AND b.price &gt;= #{q.minPrice}</if>",
            "    <if test='q.maxPrice != null'>AND b.price &lt;= #{q.maxPrice}</if>",
            "    <if test='q.minStock != null'>AND b.stock &gt;= #{q.minStock}</if>",
            "    <if test='q.status != null'>AND b.status = #{q.status}</if>",
            "    <if test='q.minSold != null'>AND COALESCE(so.sold_count, 0) &gt;= #{q.minSold}</if>",
            "</where>",
            "<choose>",
            "    <when test=\"q.orderBy == 'price_asc'\">ORDER BY b.price ASC</when>",
            "    <when test=\"q.orderBy == 'price_desc'\">ORDER BY b.price DESC</when>",
            "    <when test=\"q.orderBy == 'stock_asc'\">ORDER BY b.stock ASC</when>",
            "    <when test=\"q.orderBy == 'stock_desc'\">ORDER BY b.stock DESC</when>",
            "    <when test=\"q.orderBy == 'sold_desc'\">ORDER BY sold_count DESC</when>",
            "    <otherwise>ORDER BY b.id DESC</otherwise>",
            "</choose>",
            "</script>"
    })
    IPage<BikeVO> searchWithAnnotation(Page<?> page, @Param("q") BikeQuery query);

    /**
     * 与 statWithXml 完全等价的注解版（分组聚合统计）
     */
    @Select({
            "<script>",
            "SELECT b.category AS category,",
            "       COUNT(*)                        AS bike_count,",
            "       COALESCE(SUM(b.stock), 0)       AS total_stock,",
            "       ROUND(AVG(b.price), 2)          AS avg_price,",
            "       MAX(b.price)                    AS max_price,",
            "       MIN(b.price)                    AS min_price,",
            "       COALESCE(SUM(so.sold_count), 0) AS sold_count,",
            "       COALESCE(SUM(so.sales_amount), 0) AS sales_amount",
            "FROM t_bike b",
            "LEFT JOIN (",
            "    SELECT bike_id,",
            "           SUM(quantity)     AS sold_count,",
            "           SUM(total_amount) AS sales_amount",
            "    FROM t_order",
            "    WHERE deleted = 0 AND status &lt;&gt; '已取消'",
            "    GROUP BY bike_id",
            ") so ON so.bike_id = b.id",
            "<where>",
            "    AND b.deleted = 0",
            "    <if test='q.keyword != null and q.keyword != &quot;&quot;'>",
            "        AND (b.name LIKE CONCAT('%', #{q.keyword}, '%')",
            "          OR b.brand LIKE CONCAT('%', #{q.keyword}, '%')",
            "          OR b.description LIKE CONCAT('%', #{q.keyword}, '%'))",
            "    </if>",
            "    <if test='q.categories != null and q.categories.size() &gt; 0'>",
            "        AND b.category IN",
            "        <foreach collection='q.categories' item='c' open='(' separator=',' close=')'>#{c}</foreach>",
            "    </if>",
            "    <if test='q.brands != null and q.brands.size() &gt; 0'>",
            "        AND b.brand IN",
            "        <foreach collection='q.brands' item='b2' open='(' separator=',' close=')'>#{b2}</foreach>",
            "    </if>",
            "    <if test='q.minPrice != null'>AND b.price &gt;= #{q.minPrice}</if>",
            "    <if test='q.maxPrice != null'>AND b.price &lt;= #{q.maxPrice}</if>",
            "    <if test='q.minStock != null'>AND b.stock &gt;= #{q.minStock}</if>",
            "    <if test='q.status != null'>AND b.status = #{q.status}</if>",
            "    <if test='q.minSold != null'>AND COALESCE(so.sold_count, 0) &gt;= #{q.minSold}</if>",
            "</where>",
            "GROUP BY b.category",
            "<choose>",
            "    <when test=\"q.orderBy == 'sold_desc'\">ORDER BY sold_count DESC</when>",
            "    <when test=\"q.orderBy == 'price_desc'\">ORDER BY max_price DESC</when>",
            "    <when test=\"q.orderBy == 'price_asc'\">ORDER BY min_price ASC</when>",
            "    <otherwise>ORDER BY bike_count DESC</otherwise>",
            "</choose>",
            "</script>"
    })
    List<CategoryStatVO> statWithAnnotation(@Param("q") BikeQuery query);
}
