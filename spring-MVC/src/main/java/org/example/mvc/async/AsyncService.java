package org.example.mvc.async;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * @Async：Spring 的异步方法（与 MVC 异步请求是两套机制，但可组合使用）。
 * Controller 直接返回 CompletableFuture 时，Spring MVC 也会按异步请求处理。
 */
@Slf4j
@Service
public class AsyncService {

    @Async("taskExecutor")
    public CompletableFuture<String> slowTask(String name) {
        log.info("[AsyncService] 慢任务开始，线程={}", Thread.currentThread().getName());
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log.info("[AsyncService] 慢任务结束，线程={}", Thread.currentThread().getName());
        return CompletableFuture.completedFuture("hello " + name + "，来自 @Async 线程 " + Thread.currentThread().getName());
    }
}
