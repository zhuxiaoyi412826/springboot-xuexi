package org.example.redis.listener;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.redis.redis.RedisKeys;
import org.example.redis.redis.RedisService;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Pub/Sub 订阅者：接收站内通知（登录提醒、歌曲上新等）。
 *
 * <p>知识点：
 * <ul>
 *   <li>Publisher 用 PUBLISH，Subscriber 订阅频道即可收到广播</li>
 *   <li>Redis 的 Pub/Sub 是「即发即忘」：订阅者不在线就收不到（需要可靠投递请用 Stream）</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NoticeListener {

    private static final String NOTICE_LIST = "music:notice:list";

    private final RedisService redis;
    private final RedisMessageListenerContainer container;

    @PostConstruct
    public void subscribe() {
        try {
            container.addMessageListener((message, pattern) -> {
                String body = new String(message.getBody());
                log.info("收到通知：{}", body);
                redis.lPushAndTrim(NOTICE_LIST, body, 20);
            }, new ChannelTopic(RedisKeys.CHANNEL_NOTICE));
        } catch (Exception e) {
            log.warn("订阅通知频道失败：{}", e.getMessage());
        }
    }

    public List<String> latest() {
        return redis.lRange(NOTICE_LIST, 0, 19);
    }
}
