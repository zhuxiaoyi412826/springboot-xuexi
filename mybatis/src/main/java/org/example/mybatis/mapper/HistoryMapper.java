package org.example.mybatis.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.mybatis.dto.HistoryVO;
import org.example.mybatis.entity.ReadingHistory;

import java.util.List;

/**
 * 阅读历史 / 书架 Mapper
 */
public interface HistoryMapper {

    // ==================== 注解：简单 SQL ====================

    @Select("SELECT id, user_id, novel_id, chapter_id, chapter_no, progress, last_read_time " +
            "FROM reading_history WHERE user_id = #{userId} AND novel_id = #{novelId}")
    ReadingHistory selectByUserAndNovel(@Param("userId") Long userId, @Param("novelId") Long novelId);

    @Select("SELECT COUNT(*) FROM reading_history WHERE user_id = #{userId}")
    long countByUserId(@Param("userId") Long userId);

    @Update("UPDATE reading_history SET chapter_id = #{chapterId}, chapter_no = #{chapterNo}, " +
            "progress = #{progress}, last_read_time = NOW() WHERE id = #{id}")
    int updateProgress(ReadingHistory history);

    @Select("SELECT COUNT(*) FROM reading_history WHERE user_id = #{userId} AND novel_id = #{novelId}")
    long countByUserAndNovel(@Param("userId") Long userId, @Param("novelId") Long novelId);

    @Delete("DELETE FROM reading_history WHERE user_id = #{userId} AND novel_id = #{novelId}")
    int delete(@Param("userId") Long userId, @Param("novelId") Long novelId);

    @Delete("DELETE FROM reading_history WHERE novel_id = #{novelId}")
    int deleteByNovelId(@Param("novelId") Long novelId);

    // ==================== XML：复杂 SQL ====================

    /**
     * 新增或更新阅读进度（MySQL INSERT ... ON DUPLICATE KEY UPDATE）
     * 一条 SQL 搞定 upsert，依赖 (user_id, novel_id) 唯一索引。
     */
    int upsert(ReadingHistory history);

    /**
     * 我的书架：阅读历史 JOIN 小说 JOIN 分类，并子查出上一章/下一章序号
     */
    List<HistoryVO> selectMyHistory(@Param("userId") Long userId);
}
