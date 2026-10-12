package org.example.mybatis.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.mybatis.common.BizException;
import org.example.mybatis.common.PageResult;
import org.example.mybatis.dto.HistoryVO;
import org.example.mybatis.entity.Novel;
import org.example.mybatis.entity.ReadingHistory;
import org.example.mybatis.mapper.HistoryMapper;
import org.example.mybatis.mapper.NovelMapper;
import org.example.mybatis.plugin.PageContext;
import org.example.mybatis.service.HistoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 阅读历史服务
 *
 * <p>记录进度用的是 upsert：
 * MySQL 的 {@code INSERT ... ON DUPLICATE KEY UPDATE} 一条 SQL 搞定「有则更新、无则插入」，
 * 依赖 reading_history 上的 (user_id, novel_id) 唯一索引。
 */
@Service
@RequiredArgsConstructor
public class HistoryServiceImpl implements HistoryService {

    private final HistoryMapper historyMapper;
    private final NovelMapper novelMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void record(Long userId, Long novelId, Long chapterId, Integer chapterNo, Integer progress) {
        Novel novel = novelMapper.selectById(novelId);
        if (novel == null) {
            throw new BizException("小说不存在");
        }
        ReadingHistory history = new ReadingHistory();
        history.setUserId(userId);
        history.setNovelId(novelId);
        history.setChapterId(chapterId);
        history.setChapterNo(chapterNo == null || chapterNo < 1 ? 1 : chapterNo);
        if (progress == null) {
            progress = 0;
        }
        history.setProgress(Math.max(0, Math.min(100, progress)));
        historyMapper.upsert(history);
    }

    @Override
    public PageResult<HistoryVO> myHistory(Long userId, int pageNum, int pageSize) {
        try {
            PageContext.start(pageNum, pageSize);
            List<HistoryVO> list = historyMapper.selectMyHistory(userId);
            long total = PageContext.getTotal();
            return PageResult.of(list, total, pageNum, pageSize);
        } finally {
            PageContext.clear();
        }
    }

    @Override
    public ReadingHistory get(Long userId, Long novelId) {
        return historyMapper.selectByUserAndNovel(userId, novelId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long userId, Long novelId) {
        historyMapper.delete(userId, novelId);
    }

    @Override
    public long count(Long userId) {
        return historyMapper.countByUserId(userId);
    }
}
