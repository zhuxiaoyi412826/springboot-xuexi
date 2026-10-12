package org.example.mybatis.mapper;

import org.apache.ibatis.annotations.CacheNamespace;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.mybatis.entity.Category;

import java.util.List;

/**
 * 分类 Mapper —— 【纯注解 CRUD】的示例
 *
 * <p>知识点：
 * <ul>
 *   <li>{@code @Select/@Insert/@Update/@Delete} 直接在接口方法上写 SQL，适合简单语句</li>
 *   <li>{@code @Options(useGeneratedKeys = true, keyProperty = "id")} 主键自动回填</li>
 *   <li>{@code @CacheNamespace} 开启【二级缓存】（实体必须实现 Serializable）</li>
 * </ul>
 *
 * <p>复杂 SQL（多表、动态条件、批量）请写在 resources/mapper/*.xml 里，参见 NovelMapper。
 */
@CacheNamespace(flushInterval = 600000)
public interface CategoryMapper {

    /** 查询所有分类（二级缓存生效，10 分钟刷新一次） */
    @Select("SELECT id, name, description, sort_num, create_time, deleted " +
            "FROM category WHERE deleted = 0 ORDER BY sort_num DESC, id ASC")
    List<Category> selectAll();

    @Select("SELECT id, name, description, sort_num, create_time, deleted " +
            "FROM category WHERE id = #{id} AND deleted = 0")
    Category selectById(Long id);

    /**
     * 新增，主键自增回填到对象的 id 字段
     */
    @Insert("INSERT INTO category (name, description, sort_num) VALUES (#{name}, #{description}, #{sortNum})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Category category);

    /**
     * 修改。这里演示 flushCache：写操作默认会清空该命名空间的二级缓存
     */
    @Update("UPDATE category SET name = #{name}, description = #{description}, sort_num = #{sortNum} " +
            "WHERE id = #{id} AND deleted = 0")
    int update(Category category);

    /** 逻辑删除 */
    @Delete("UPDATE category SET deleted = 1 WHERE id = #{id}")
    int deleteById(Long id);

    // ==================== XML：复杂 SQL ====================

    /**
     * 分类连同其下的小说一起查出（XML 的 {@code <collection>} 一对多映射），
     * 一次 SQL 搞定，避免 N+1 查询。
     */
    List<Category> selectWithNovels();

    /**
     * 只返回至少有一本小说的分类（XML 中 GROUP BY + HAVING 演示）
     */
    List<Category> selectNonEmpty();

    /** 统计：演示 SQL 聚合函数 COUNT */
    @Select("SELECT COUNT(*) FROM category WHERE deleted = 0")
    long count();

    /** 按分类名精确查重（新增/修改时校验唯一性） */
    @Select("SELECT COUNT(*) FROM category WHERE name = #{name} AND deleted = 0 " +
            "AND (#{excludeId} IS NULL OR id &lt;&gt; #{excludeId})")
    long countByName(@Param("excludeId") Long excludeId, @Param("name") String name);
}
