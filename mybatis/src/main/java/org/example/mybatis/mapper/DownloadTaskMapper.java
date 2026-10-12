package org.example.mybatis.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.mybatis.dto.TaskQuery;
import org.example.mybatis.entity.DownloadTask;

import java.util.List;
import java.util.Map;

/**
 * 下载任务 Mapper
 */
public interface DownloadTaskMapper {

    // ==================== 注解：简单 SQL ====================

    @Select("SELECT id, task_no, user_id, novel_id, status, total_chapter, done_chapter, file_size, " +
            "error_msg, create_time, start_time, finish_time, update_time FROM download_task WHERE id = #{id}")
    DownloadTask selectById(@Param("id") Long id);

    @Select("SELECT id, task_no, user_id, novel_id, status, total_chapter, done_chapter, file_size, " +
            "error_msg, create_time, start_time, finish_time, update_time FROM download_task WHERE task_no = #{taskNo}")
    DownloadTask selectByTaskNo(@Param("taskNo") String taskNo);

    @Insert("INSERT INTO download_task (task_no, user_id, novel_id, status, total_chapter, done_chapter, file_size) " +
            "VALUES (#{taskNo}, #{userId}, #{novelId}, #{status}, #{totalChapter}, #{doneChapter}, #{fileSize})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(DownloadTask task);

    /** 更新下载进度 */
    @Update("UPDATE download_task SET done_chapter = #{doneChapter}, file_size = #{fileSize}, " +
            "status = #{status} WHERE id = #{id}")
    int updateProgress(@Param("id") Long id, @Param("doneChapter") Integer doneChapter,
                       @Param("fileSize") Long fileSize, @Param("status") String status);

    /** 状态流转：这里用带条件的 UPDATE 保证并发安全（类似乐观锁 CAS） */
    @Update("UPDATE download_task SET status = #{newStatus} WHERE id = #{id} AND status = #{oldStatus}")
    int updateStatusCas(@Param("id") Long id, @Param("oldStatus") String oldStatus, @Param("newStatus") String newStatus);

    /** 通用状态流转：finished=1 时同时写入结束时间 */
    @Update("UPDATE download_task SET status = #{status}, " +
            "start_time = CASE WHEN #{started} = 1 AND start_time IS NULL THEN NOW() ELSE start_time END, " +
            "finish_time = CASE WHEN #{finished} = 1 THEN NOW() ELSE finish_time END, " +
            "error_msg = #{errorMsg} " +
            "WHERE id = #{id}")
    int setStatus(@Param("id") Long id, @Param("status") String status,
                  @Param("started") int started, @Param("finished") int finished,
                  @Param("errorMsg") String errorMsg);

    /** 该小说是否已有进行中的任务 */
    @Select("SELECT COUNT(*) FROM download_task WHERE user_id = #{userId} AND novel_id = #{novelId} " +
            "AND status IN ('QUEUED', 'DOWNLOADING', 'PAUSED')")
    long countActive(@Param("userId") Long userId, @Param("novelId") Long novelId);

    /**
     * GROUP BY 分组统计各状态任务数
     * 返回 [{status: 'COMPLETED', cnt: 3}, ...]
     */
    @Select("SELECT status, COUNT(*) AS cnt FROM download_task GROUP BY status")
    List<Map<String, Object>> countGroupByStatus();

    // ==================== XML：复杂 SQL ====================

    /**
     * 多条件查询任务列表（JOIN 小说表）
     */
    List<org.example.mybatis.dto.DownloadTaskVO> selectByCondition(TaskQuery query);

    /**
     * 批量取消任务（XML foreach）
     */
    int batchCancel(@Param("ids") List<Long> ids, @Param("userId") Long userId);
}
