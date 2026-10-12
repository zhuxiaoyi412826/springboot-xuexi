package org.example.mybatis.runner;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.mybatis.entity.DownloadTask;
import org.example.mybatis.mapper.DownloadTaskMapper;
import org.example.mybatis.mapper.NovelMapper;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 下载任务执行器
 *
 * <p>每个任务一个工作线程，循环推进已下载章节数；线程每一步都会重新读取任务状态，
 * 于是「暂停 / 继续 / 停止」只要改数据库状态就能生效 —— 这就是状态机 + 轮询的典型做法。
 *
 * <p>状态流转：
 * <pre>
 *   DOWNLOADING --暂停--> PAUSED --继续--> DOWNLOADING
 *        |                   |
 *        +--------停止-------+--> CANCELLED
 *        |
 *        +--> 章节全部下载完 --> COMPLETED（同时小说下载数 +1）
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DownloadRunner {

    /** 模拟每章下载耗时 */
    private static final long PER_CHAPTER_MS = 400L;
    /** 模拟每章字节数 */
    private static final long BYTES_PER_CHAPTER = 5120L;

    private final ExecutorService pool = Executors.newFixedThreadPool(4);

    /** 正在执行的任务，避免重复提交线程 */
    private final Set<Long> running = ConcurrentHashMap.newKeySet();

    private final DownloadTaskMapper taskMapper;
    private final NovelMapper novelMapper;

    /** 提交任务到线程池（同一个任务不会重复提交） */
    public void submit(Long taskId) {
        if (!running.add(taskId)) {
            return;
        }
        pool.submit(() -> {
            try {
                run(taskId);
            } finally {
                running.remove(taskId);
            }
        });
    }

    private void run(Long taskId) {
        log.info("下载任务开始：taskId={}", taskId);
        try {
            while (true) {
                DownloadTask task = taskMapper.selectById(taskId);
                if (task == null) {
                    return;
                }
                String status = task.getStatus();
                if ("COMPLETED".equals(status) || "CANCELLED".equals(status) || "FAILED".equals(status)) {
                    log.info("任务终止，status={}, taskId={}", status, taskId);
                    return;
                }
                // 暂停：等待用户点「继续」，线程不退出
                if ("PAUSED".equals(status)) {
                    sleep();
                    continue;
                }
                if (!"DOWNLOADING".equals(status)) {
                    return;
                }

                int total = task.getTotalChapter() == null ? 0 : task.getTotalChapter();
                int done = task.getDoneChapter() == null ? 0 : task.getDoneChapter();

                if (done >= total) {
                    // 下载完成
                    taskMapper.setStatus(taskId, "COMPLETED", 1, 1, null);
                    novelMapper.incrDownloadCount(task.getNovelId());
                    log.info("下载完成：taskId={}", taskId);
                    return;
                }

                int next = done + 1;
                taskMapper.updateProgress(taskId, next, next * BYTES_PER_CHAPTER, "DOWNLOADING");
                sleep();
            }
        } catch (Exception e) {
            log.error("下载任务异常：taskId={}", taskId, e);
            taskMapper.setStatus(taskId, "FAILED", 0, 1, String.valueOf(e.getMessage()));
        }
    }

    private void sleep() {
        try {
            Thread.sleep(PER_CHAPTER_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("下载被中断", e);
        }
    }
}
