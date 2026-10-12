package org.example.security.service;

import lombok.RequiredArgsConstructor;
import org.example.security.repository.CommentRepository;
import org.example.security.repository.FavoriteRepository;
import org.example.security.repository.LoginLogRepository;
import org.example.security.repository.UserRepository;
import org.example.security.repository.VideoRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/** 管理端概览统计 */
@Service
@RequiredArgsConstructor
public class StatsService {

    private final UserRepository userRepository;
    private final VideoRepository videoRepository;
    private final CommentRepository commentRepository;
    private final FavoriteRepository favoriteRepository;
    private final LoginLogRepository loginLogRepository;

    public Map<String, Object> overview() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("users", userRepository.count());
        data.put("videos", videoRepository.count());
        data.put("publicVideos", videoRepository.countByStatus("PUBLIC"));
        data.put("comments", commentRepository.count());
        data.put("favorites", favoriteRepository.count());
        data.put("loginLogs", loginLogRepository.count());
        return data;
    }

    /** 最近登录日志（管理端） */
    public java.util.List<org.example.security.entity.LoginLog> latestLogs(int size) {
        return loginLogRepository.searchAdmin(
                null, org.springframework.data.domain.PageRequest.of(0, Math.min(size, 50),
                        org.springframework.data.domain.Sort.by(
                                org.springframework.data.domain.Sort.Direction.DESC, "id")))
                .getContent();
    }
}
