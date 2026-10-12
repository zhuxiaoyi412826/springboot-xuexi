package org.example.mybatis.service;

import org.example.mybatis.common.PageResult;
import org.example.mybatis.dto.DownloadTaskVO;
import org.example.mybatis.dto.TaskQuery;

import java.util.List;
import java.util.Map;

public interface DownloadService {

    /** 创建并立即开始下载 */
    DownloadTaskVO create(Long userId, Long novelId);

    /** 暂停 */
    void pause(Long userId, Long taskId);

    /** 继续 */
    void resume(Long userId, Long taskId);

    /** 停止（取消） */
    void cancel(Long userId, Long taskId);

    /** 批量停止进行中的任务 */
    void cancelAll(Long userId);

    /** 重新下载（换个任务号） */
    DownloadTaskVO restart(Long userId, Long taskId);

    PageResult<DownloadTaskVO> page(TaskQuery query, int pageNum, int pageSize);

    DownloadTaskVO get(Long taskId);

    /** 各状态任务数统计（GROUP BY） */
    Map<String, Object> statusStat();

    List<Map<String, Object>> countGroupByStatus();
}
