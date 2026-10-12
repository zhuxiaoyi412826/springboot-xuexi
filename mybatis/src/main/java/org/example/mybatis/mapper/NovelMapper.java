package org.example.mybatis.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.mybatis.dto.NovelDetailVO;
import org.example.mybatis.dto.NovelQuery;
import org.example.mybatis.dto.NovelVO;
import org.example.mybatis.entity.Novel;

import java.util.List;

/**
 * 小说 Mapper
 *
 * <p>本项目最核心的 Mapper，复杂 SQL 全部写在 resources/mapper/NovelMapper.xml：
 * <ul>
 *   <li>多表 JOIN + resultMap + association</li>
 *   <li>动态 SQL（if / choose / foreach / set / trim / bind / sql 片段）</li>
 *   <li>SQL 函数（COUNT/SUM/AVG/MAX/MIN/IFNULL/CONCAT/DATE_FORMAT）</li>
 *   <li>子查询、EXISTS</li>
 *   <li>二级缓存 {@code <cache>}</li>
 * </ul>
 */
public interface NovelMapper {

    // ==================== 注解：简单 SQL ====================

    @Select("SELECT id, title, author, category_id, tags, cover_url, description, word_count, chapter_count, " +
            "status, view_count, favorite_count, download_count, file_path, create_time, update_time, deleted " +
            "FROM novel WHERE id = #{id} AND deleted = 0")
    Novel selectById(@Param("id") Long id);

    @Select("SELECT COUNT(*) FROM novel WHERE deleted = 0")
    long count();

    /** 该分类下还有多少本小说（删除分类前校验） */
    @Select("SELECT COUNT(*) FROM novel WHERE category_id = #{categoryId} AND deleted = 0")
    long selectWithNovelCountByCategory(@Param("categoryId") Long categoryId);

    /**
     * 新增小说（主键回填）
     */
    @Insert("INSERT INTO novel (title, author, category_id, tags, cover_url, description, status) " +
            "VALUES (#{title}, #{author}, #{categoryId}, #{tags,typeHandler=org.example.mybatis.type.TagsTypeHandler}, " +
            "#{coverUrl}, #{description}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Novel novel);

    /**
     * 浏览量 +1（原子自增，避免并发覆盖）
     *
     * <p>flushCache = FALSE：浏览是最频繁的写操作，若每次都清空整个命名空间的二级缓存，
     * 缓存就形同虚设。它只自增一个计数器，不影响其它查询结果。
     */
    @Update("UPDATE novel SET view_count = view_count + 1 WHERE id = #{id}")
    @Options(flushCache = Options.FlushCachePolicy.FALSE)
    int incrViewCount(@Param("id") Long id);

    /** 收藏数 ±1 */
    @Update("UPDATE novel SET favorite_count = favorite_count + #{delta} WHERE id = #{id} AND favorite_count + #{delta} >= 0")
    int changeFavoriteCount(@Param("id") Long id, @Param("delta") int delta);

    /** 下载数 +1 */
    @Update("UPDATE novel SET download_count = download_count + 1 WHERE id = #{id}")
    int incrDownloadCount(@Param("id") Long id);

    /** 逻辑删除 */
    @Update("UPDATE novel SET deleted = 1 WHERE id = #{id}")
    int logicDelete(@Param("id") Long id);

    // ==================== XML：复杂 SQL ====================

    /**
     * 动态更新（XML 中 {@code <set>} 演示：只更新传了的字段）
     */
    int updateSelective(Novel novel);

    /**
     * 多条件检索 + 多表关联 + 自定义 TypeHandler 映射 tags
     */
    List<NovelVO> selectByCondition(NovelQuery query);

    /**
     * 小说详情：多表关联 + 子查询 + 我的阅读进度
     */
    NovelDetailVO selectDetail(@Param("novelId") Long novelId, @Param("userId") Long userId);

    /**
     * 热门小说 TOP N：ORDER BY + LIMIT
     */
    List<NovelVO> selectHotTop(@Param("limit") int limit, @Param("categoryId") Long categoryId);

    /**
     * 批量删除（XML foreach）
     */
    int batchLogicDelete(@Param("ids") List<Long> ids);

    /**
     * 批量插入（XML foreach，配合事务使用）
     */
    int batchInsert(@Param("list") List<Novel> list);

    /**
     * 修正聚合字段：把章节表的统计结果回写到小说表（UPDATE JOIN）
     */
    int refreshStatByChapter(@Param("novelId") Long novelId);
}
