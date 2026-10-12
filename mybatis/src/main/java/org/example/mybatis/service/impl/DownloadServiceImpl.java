package org.example.mybatis.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.mybatis.common.BizException;
import org.example.mybatis.common.PageResult;
import org.example.mybatis.dto.DownloadTaskVO;
import org.example.mybatis.dto.TaskQuery;
import org.example.mybatis.entity.DownloadTask;
import org.example.mybatis.entity.Novel;
import org.example.mybatis.mapper.ChapterMapper;
import org.example.mybatis.mapper.DownloadTaskMapper;
import org.example.mybatis.mapper.NovelMapper;
import org.example.mybatis.plugin.PageContext;
import org.example.mybatis.runner.DownloadRunner;
import org.example.mybatis.service.DownloadService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class DownloadServiceImpl implements DownloadService {

    private final DownloadTaskMapper taskMapper;
    private final NovelMapper novelMapper;
    private final ChapterMapper chapterMapper;
    private final DownloadRunner downloadRunner;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DownloadTaskVO create(Long userId, Long novelId) {
        Novel novel = novelMapper.selectById(novelId);
        if (novel == null) {
            throw new BizException("小说不存在");
        }
        // 同一本小说已有进行中的任务就不再重复创建
        if (taskMapper.countActive(userId, novelId) > 0) {
            throw new BizException("该小说正在下载中，请勿重复添加");
        }
        int total = (int) chapterMapper.countByNovelId(novelId);

        DownloadTask task = new DownloadTask();
        task.setTaskNo(generateTaskNo(userId));
        task.setUserId(userId);
        task.setNovelId(novelId);
        task.setStatus("DOWNLOADING");
        task.setTotalChapter(total);
        task.setDoneChapter(0);
        task.setFileSize(0L);
        taskMapper.insert(task);

        // 提交到线程池异步推进
        downloadRunner.submit(task.getId());
        return toVO(task, novel);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pause(Long userId, Long taskId) {
        DownloadTask task = requireOwn(userId, taskId);
        if (!"DOWNLOADING".equals(task.getStatus())) {
            throw new BizException("只有下载中的任务才能暂停");
        }
        // CAS 更新：并发下只有一个人能改成功
        if (taskMapper.updateStatusCas(taskId, "DOWNLOADING", "PAUSED") == 0) {
            throw new BizException("任务状态已变化，请刷新后重试");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resume(Long userId, Long taskId) {
        DownloadTask task = requireOwn(userId, taskId);
        if (!"PAUSED".equals(task.getStatus())) {
            throw new BizException("只有暂停中的任务才能继续");
        }
        if (taskMapper.updateStatusCas(taskId, "PAUSED", "DOWNLOADING") == 0) {
            throw new BizException("任务状态已变化，请刷新后重试");
        }
        // 重新拉活线程（幂等：已在跑的不会重复提交）
        downloadRunner.submit(taskId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long userId, Long taskId) {
        DownloadTask task = requireOwn(userId, taskId);
        if ("COMPLETED".equals(task.getStatus())) {
            throw new BizException("已完成的任务无需停止");
        }
        if ("CANCELLED".equals(task.getStatus())) {
            throw new BizException("任务已停止");
        }
        taskMapper.setStatus(taskId, "CANCELLED", 0, 1, "用户主动停止");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelAll(Long userId) {
        TaskQuery query = new TaskQuery();
        query.setUserId(userId);
        try {
            PageContext.start(1, 1000);
            List<DownloadTaskVO> list = taskMapper.selectByCondition(query);
            List<Long> ids = new ArrayList<>();
            for (DownloadTaskVO vo : list) {
                if ("QUEUED".equals(vo.getStatus()) || "DOWNLOADING".equals(vo.getStatus())
                        || "PAUSED".equals(vo.getStatus())) {
                    ids.add(vo.getId());
                }
            }
            if (!ids.isEmpty()) {
                // 批量取消走 XML 的 foreach
                taskMapper.batchCancel(ids, userId);
            }
        } finally {
            PageContext.clear();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DownloadTaskVO restart(Long userId, Long taskId) {
        DownloadTask task = requireOwn(userId, taskId);
        Novel novel = novelMapper.selectById(task.getNovelId());
        return create(userId, novel == null ? task.getNovelId() : novel.getId());
    }

    @Override
    public PageResult<DownloadTaskVO> page(TaskQuery query, int pageNum, int pageSize) {
        try {
            PageContext.start(pageNum, pageSize);
            List<DownloadTaskVO> list = taskMapper.selectByCondition(query == null ? new TaskQuery() : query);
            long total = PageContext.getTotal();
            return PageResult.of(list, total, pageNum, pageSize);
        } finally {
            PageContext.clear();
        }
    }

    @Override
    public DownloadTaskVO get(Long taskId) {
        DownloadTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BizException("任务不存在");
        }
        Novel novel = novelMapper.selectById(task.getNovelId());
        return toVO(task, novel);
    }

    @Override
    public Map<String, Object> statusStat() {
        List<Map<String, Object>> raw = taskMapper.countGroupByStatus();
        Map<String, Object> res = new HashMap<>();
        long sum = 0;
        for (Map<String, Object> row : raw) {
            Object status = row.get("status");
            Object cnt = row.get("cnt");
            long num = cnt == null ? 0L : ((Number) cnt).longValue();
            if (status != null) {
                res.put(String.valueOf(status), num);
                sum += num;
            }
        }
        res.put("TOTAL", sum);
        return res;
    }

    @Override
    public List<Map<String, Object>> countGroupByStatus() {
        return taskMapper.countGroupByStatus();
    }

    /** 校验任务归属 */
    private DownloadTask requireOwn(Long userId, Long taskId) {
        DownloadTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BizException("任务不存在");
        }
        if (!task.getUserId().equals(userId)) {
            throw new BizException("只能操作自己的下载任务");
        }
        return task;
    }

    private DownloadTaskVO toVO(DownloadTask task, Novel novel) {
        DownloadTaskVO vo = new DownloadTaskVO();
        vo.setId(task.getId());
        vo.setTaskNo(task.getTaskNo());
        vo.setNovelId(task.getNovelId());
        vo.setStatus(task.getStatus());
        vo.setTotalChapter(task.getTotalChapter());
        vo.setDoneChapter(task.getDoneChapter());
        vo.setFileSize(task.getFileSize());
        vo.setErrorMsg(task.getErrorMsg());
        vo.setCreateTime(task.getCreateTime());
        vo.setStartTime(task.getStartTime());
        vo.setFinishTime(task.getFinishTime());
        if (novel != null) {
            vo.setTitle(novel.getTitle());
            vo.setAuthor(novel.getAuthor());
            vo.setCoverUrl(novel.getCoverUrl());
        }
        return vo;
    }

    private String generateTaskNo(Long userId) {
        return "DL" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"))
                + String.format("%02d", userId % 100)
                + ThreadLocalRandom.current().nextInt(100, 999);
    }
}
