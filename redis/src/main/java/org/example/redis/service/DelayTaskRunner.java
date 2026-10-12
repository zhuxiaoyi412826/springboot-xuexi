package org.example.redis.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.redis.cache.DelayQueueService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 延迟队列消费者（定时轮询 ZSet）。
 *
 * <p>任务格式：{@code song:{id}:online} 表示「N 秒后把这首歌上架」。
 * 管理端下架歌曲时可以顺便投递一个「1 分钟后自动上架」的任务来观察效果。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DelayTaskRunner {

    private final DelayQueueService delayQueue;
    private final SongService songService;

    @Scheduled(fixedDelay = 10_000, initialDelay = 10_000)
    public void poll() {
        try {
            var tasks = delayQueue.popDue(20);
            for (String task : tasks) {
                try {
                    handle(task);
                    log.info("延迟任务执行成功：{}", task);
                } catch (Exception e) {
                    log.warn("延迟任务执行失败：{} -> {}", task, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.debug("轮询延迟队列失败：{}", e.getMessage());
        }
    }

    private void handle(String task) {
        String[] arr = task.split(":");
        if (arr.length == 3 && "song".equals(arr[0]) && "online".equals(arr[2])) {
            songService.setStatus(Long.valueOf(arr[1]), 1);
        }
    }
}
