package org.example.security.service;

import lombok.RequiredArgsConstructor;
import org.example.security.common.BizException;
import org.example.security.dto.CommentVO;
import org.example.security.entity.Comment;
import org.example.security.repository.CommentRepository;
import org.example.security.repository.VideoRepository;
import org.example.security.security.PermissionChecker;
import org.example.security.security.SecurityUtil;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** 评论业务：演示方法级权限 + 对象级权限（只有作者本人或管理员能删评论） */
@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final VideoRepository videoRepository;
    private final PermissionChecker permissionChecker;

    public List<CommentVO> list(Long videoId) {
        Authentication auth = SecurityUtil.auth();
        return commentRepository.findByVideoIdAndStatusOrderByCreateTimeDesc(videoId, 0)
                .stream().map(c -> {
                    CommentVO vo = CommentVO.from(c);
                    vo.setCanDelete(permissionChecker.canDeleteComment(c.getId(), auth));
                    return vo;
                }).toList();
    }

    /** 发评论：细粒度权限 comment:write（由 ROLE_USER 派生） */
    @Transactional
    @PreAuthorize("hasAuthority('comment:write')")
    public CommentVO add(Long videoId, String content) {
        if (content == null || content.isBlank()) {
            throw new BizException("评论内容不能为空");
        }
        videoRepository.findById(videoId).orElseThrow(() -> new BizException("视频不存在"));

        Comment comment = new Comment();
        comment.setVideoId(videoId);
        comment.setUserId(SecurityUtil.currentUserId());
        comment.setUsername(SecurityUtil.currentUsername());
        comment.setContent(content.trim());
        comment.setStatus(0);
        comment.setCreateTime(LocalDateTime.now());
        return CommentVO.from(commentRepository.save(comment));
    }

    /** 删评论：对象级权限（hasPermission → 自定义 PermissionEvaluator） */
    @Transactional
    @PreAuthorize("hasPermission(#id, 'COMMENT', 'delete')")
    public void remove(Long id) {
        Comment comment = commentRepository.findById(id).orElseThrow(() -> new BizException("评论不存在"));
        comment.setStatus(1);
        commentRepository.save(comment);
    }

    public List<CommentVO> mine() {
        Long uid = SecurityUtil.currentUserId();
        if (uid == null) {
            return List.of();
        }
        return commentRepository.findByUserIdOrderByCreateTimeDesc(uid).stream()
                .filter(c -> c.getStatus() == 0)
                .map(c -> {
                    CommentVO vo = CommentVO.from(c);
                    vo.setCanDelete(true);
                    return vo;
                }).toList();
    }
}
