package org.example.mybatis.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.mybatis.dto.ChapterQuery;
import org.example.mybatis.entity.Chapter;

import java.util.List;

/**
 * 章节 Mapper
 */
public interface ChapterMapper {

    // ==================== 注解：简单 CRUD ====================

    /** 目录列表：不查 content，避免大字段拖慢 */
    @Select("SELECT id, novel_id, chapter_no, title, word_count, create_time " +
            "FROM chapter WHERE novel_id = #{novelId} ORDER BY chapter_no ASC")
    List<Chapter> selectCatalogByNovelId(@Param("novelId") Long novelId);

    /** 阅读：查单章正文 */
    @Select("SELECT id, novel_id, chapter_no, title, content, word_count, create_time " +
            "FROM chapter WHERE id = #{id}")
    Chapter selectById(@Param("id") Long id);

    @Select("SELECT id, novel_id, chapter_no, title, content, word_count, create_time " +
            "FROM chapter WHERE novel_id = #{novelId} AND chapter_no = #{chapterNo}")
    Chapter selectByNo(@Param("novelId") Long novelId, @Param("chapterNo") Integer chapterNo);

    @Insert("INSERT INTO chapter (novel_id, chapter_no, title, content, word_count) " +
            "VALUES (#{novelId}, #{chapterNo}, #{title}, #{content}, #{wordCount})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Chapter chapter);

    @Update("UPDATE chapter SET title = #{title}, content = #{content}, word_count = #{wordCount} " +
            "WHERE id = #{id}")
    int update(Chapter chapter);

    @Delete("DELETE FROM chapter WHERE id = #{id}")
    int deleteById(@Param("id") Long id);

    @Delete("DELETE FROM chapter WHERE novel_id = #{novelId}")
    int deleteByNovelId(@Param("novelId") Long novelId);

    @Select("SELECT COUNT(*) FROM chapter WHERE novel_id = #{novelId}")
    long countByNovelId(@Param("novelId") Long novelId);

    // ==================== XML：复杂 SQL ====================

    /**
     * 批量插入章节：一条 INSERT 插入多行（XML foreach），
     * 这是上传 TXT 自动分章时性能的关键。
     */
    int batchInsert(@Param("list") List<Chapter> list);

    /**
     * 动态条件查询章节
     */
    List<Chapter> selectByCondition(ChapterQuery query);

    /**
     * 批量删除指定章节
     */
    int batchDelete(@Param("ids") List<Long> ids);
}
