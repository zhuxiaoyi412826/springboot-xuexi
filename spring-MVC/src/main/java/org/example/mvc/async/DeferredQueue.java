package org.example.mvc.async;

import lombok.extern.slf4j.Slf4j;
import org.example.mvc.common.Result;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.async.DeferredResult;

import java.time.LocalTime;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 模拟「消息队列回调」：请求挂起 -> 后台线程稍后设置结果 -> 响应返回。
 * DeferredResult 与 Callable 的区别：结果可以由任意线程在任意时刻设置。
 */
@Slf4j
@Component
public class DeferredQueue {

    private final ConcurrentLinkedQueue<DeferredResult<Result>> queue = new ConcurrentLinkedQueue<>();

    public void offer(DeferredResult<Result> result) {
        queue.offer(result);
        log.info("[DeferredQueue] 入队，当前排队数={}", queue.size());
    }

    /** 每秒消费一次队列 */
    @Scheduled(fixedDelay = 1000)
    public void drain() {
        DeferredResult<Result> result;
        int count = 0;
        while ((result = queue.poll()) != null) {
            result.setResult(Result.ok("DeferredResult 延迟结果 @" + LocalTime.now()));
            count++;
        }
        if (count > 0) {
            log.info("[DeferredQueue] 本轮唤醒 {} 个挂起请求", count);
        }
    }
}
