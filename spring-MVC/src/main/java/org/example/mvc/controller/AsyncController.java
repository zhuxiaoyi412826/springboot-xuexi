package org.example.mvc.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.mvc.async.AsyncService;
import org.example.mvc.async.DeferredQueue;
import org.example.mvc.common.Result;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.context.request.async.WebAsyncTask;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalTime;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

/**
 * 异步请求：Callable / WebAsyncTask / DeferredResult / ResponseBodyEmitter /
 * SseEmitter / StreamingResponseBody / CompletableFuture。
 *
 * <p>共同点：Controller 线程（Tomcat 线程）立即释放，真正的工作在其它线程完成，
 * 响应通过 ASYNC 分派二次写回。</p>
 */
@Slf4j
@RestController
@RequestMapping("/mvc/async")
@RequiredArgsConstructor
public class AsyncController {

    private final DeferredQueue deferredQueue;
    private final AsyncService asyncService;
    private final TaskExecutor mvcAsyncExecutor;

    /** 1. Callable：最简单，返回值由 Spring 用 AsyncTaskExecutor 执行 */
    @GetMapping("/callable")
    public Callable<Result> callable() {
        log.info("[AsyncController] callable 进入，线程={}", Thread.currentThread().getName());
        return () -> {
            Thread.sleep(1500);
            log.info("[AsyncController] callable 执行完，线程={}", Thread.currentThread().getName());
            return Result.ok("Callable 结果 @" + LocalTime.now());
        };
    }

    /** 2. WebAsyncTask：可以设置超时时间与超时/完成回调 */
    @GetMapping("/task")
    public WebAsyncTask<Result> task() {
        WebAsyncTask<Result> task = new WebAsyncTask<>(3000, (Callable<Result>) () -> {
            Thread.sleep(1000);
            return Result.ok("WebAsyncTask 结果 @" + LocalTime.now());
        });
        task.onTimeout(() -> Result.fail(504, "处理超时（onTimeout 回调）"));
        task.onCompletion(() -> log.info("[AsyncController] WebAsyncTask 完成回调"));
        return task;
    }

    /** 3. DeferredResult：结果由其它线程（此处是定时任务）设置 */
    @GetMapping("/deferred")
    public DeferredResult<Result> deferred() {
        DeferredResult<Result> result = new DeferredResult<>(10000L, Result.fail(504, "DeferredResult 超时兜底"));
        result.onTimeout(() -> log.warn("[AsyncController] DeferredResult 超时"));
        result.onCompletion(() -> log.info("[AsyncController] DeferredResult 完成"));
        deferredQueue.offer(result);
        return result;
    }

    /** 4. ResponseBodyEmitter：多次向客户端推送数据（长连接响应） */
    @GetMapping("/emitter")
    public ResponseBodyEmitter emitter() {
        ResponseBodyEmitter emitter = new ResponseBodyEmitter(15000L);
        mvcAsyncExecutor.execute(() -> {
            try {
                for (int i = 1; i <= 5; i++) {
                    emitter.send("第 " + i + " 条推送 @" + LocalTime.now() + "\n");
                    Thread.sleep(400);
                }
                emitter.complete();
            } catch (IOException | InterruptedException e) {
                emitter.completeWithError(e);
            }
        });
        return emitter;
    }

    /** 5. SseEmitter：Server-Sent Events（text/event-stream） */
    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sse() {
        SseEmitter emitter = new SseEmitter(30000L);
        mvcAsyncExecutor.execute(() -> {
            try {
                for (int i = 1; i <= 10; i++) {
                    emitter.send(SseEmitter.event()
                            .name("tick")
                            .data("服务器时间 " + LocalTime.now()));
                    Thread.sleep(1000);
                }
                emitter.complete();
            } catch (IOException | InterruptedException e) {
                emitter.completeWithError(e);
            }
        });
        return emitter;
    }

    /** 6. StreamingResponseBody：适合大文件流式下载，边生成边写 */
    @GetMapping("/stream")
    public org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody stream() {
        return out -> {
            for (int i = 1; i <= 5; i++) {
                out.write(("流式第 " + i + " 行\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
                out.flush();
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        };
    }

    /** 7. CompletableFuture + @Async */
    @GetMapping("/future")
    public CompletableFuture<Result> future(@RequestParam(defaultValue = "Spring") String name) {
        return asyncService.slowTask(name).thenApply(Result::ok);
    }
}
