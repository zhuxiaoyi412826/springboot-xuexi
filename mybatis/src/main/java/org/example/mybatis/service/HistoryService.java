package org.example.mybatis.service;

import org.example.mybatis.common.PageResult;
import org.example.mybatis.dto.HistoryVO;
import org.example.mybatis.entity.ReadingHistory;

public interface HistoryService {

    /** 记录阅读进度（有则更新、无则插入） */
    void record(Long userId, Long novelId, Long chapterId, Integer chapterNo, Integer progress);

    PageResult<HistoryVO> myHistory(Long userId, int pageNum, int pageSize);

    ReadingHistory get(Long userId, Long novelId);

    void remove(Long userId, Long novelId);

    long count(Long userId);
}
