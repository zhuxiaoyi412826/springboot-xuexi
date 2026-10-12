package org.example.redis.service;

import lombok.RequiredArgsConstructor;
import org.example.redis.common.BizException;
import org.example.redis.entity.User;
import org.example.redis.mapper.UserMapper;
import org.example.redis.redis.RedisKeys;
import org.example.redis.redis.RedisService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 签到（Bitmap 的经典场景）。
 *
 * <p>一个月 31 天只需要 31 个 bit（不到 4 字节）就能存下整个月的签到记录，
 * 一年也就 365 bit ≈ 46 字节/人，这是 Bitmap 相对于 Set 的巨大优势。
 */
@Service
@RequiredArgsConstructor
public class SignService {

    private static final DateTimeFormatter YM = DateTimeFormatter.ofPattern("yyyyMM");

    private final RedisService redis;
    private final UserMapper userMapper;

    /** 今日签到 */
    public Map<String, Object> sign(Long userId) {
        LocalDate today = LocalDate.now();
        String key = String.format(RedisKeys.SIGN, userId, today.format(YM));
        int day = today.getDayOfMonth();
        // offset 从 0 开始
        if (Boolean.TRUE.equals(redis.getBit(key, day - 1))) {
            throw new BizException("今天已经签到过了～");
        }
        redis.setBit(key, day - 1, true);
        User user = userMapper.selectById(userId);
        if (user != null) {
            user.setSignDays((user.getSignDays() == null ? 0 : user.getSignDays()) + 1);
            userMapper.updateById(user);
        }
        return monthInfo(userId);
    }

    /** 本月签到情况 */
    public Map<String, Object> monthInfo(Long userId) {
        LocalDate now = LocalDate.now();
        String key = String.format(RedisKeys.SIGN, userId, now.format(YM));
        List<Integer> signedDays = new ArrayList<>();
        int daysInMonth = now.lengthOfMonth();
        for (int i = 0; i < daysInMonth; i++) {
            if (Boolean.TRUE.equals(redis.getBit(key, i))) {
                signedDays.add(i + 1);
            }
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("month", now.format(YM));
        data.put("signedDays", signedDays);
        data.put("total", signedDays.size());
        data.put("continuous", continuousDays(signedDays, now.getDayOfMonth()));
        data.put("bitCount", redis.bitCount(key));
        return data;
    }

    /** 今日是否已签到 */
    public boolean signedToday(Long userId) {
        LocalDate now = LocalDate.now();
        String key = String.format(RedisKeys.SIGN, userId, now.format(YM));
        return Boolean.TRUE.equals(redis.getBit(key, now.getDayOfMonth() - 1));
    }

    /** 从今天往前数连续签到天数 */
    private int continuousDays(List<Integer> signedDays, int today) {
        int count = 0;
        for (int d = today; d >= 1; d--) {
            if (signedDays.contains(d)) {
                count++;
            } else {
                break;
            }
        }
        return count;
    }
}
