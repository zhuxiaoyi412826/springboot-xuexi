package org.example.security.security;

import lombok.RequiredArgsConstructor;
import org.example.security.entity.Comment;
import org.example.security.entity.Video;
import org.example.security.repository.CommentRepository;
import org.example.security.repository.VideoRepository;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * 对象级权限（ACL 的轻量实现）：让 {@code @PreAuthorize("hasPermission(#id, 'VIDEO', 'edit')")} 生效。
 *
 * <p>与「URL 级权限」的区别：URL 级只能控制「能不能访问某个接口」，
 * 对象级能控制「能不能操作这一条数据」。</p>
 */
@Component
@RequiredArgsConstructor
public class VideoPermissionEvaluator implements PermissionEvaluator {

    private final VideoRepository videoRepository;
    private final CommentRepository commentRepository;

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        // 形如 @PreAuthorize("hasPermission(#video, 'edit')") 的用法
        if (targetDomainObject instanceof Video video) {
            return checkVideo(video, String.valueOf(permission), authentication);
        }
        return false;
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        String perm = String.valueOf(permission);
        return switch (targetType.toUpperCase()) {
            case "VIDEO" -> checkVideo(videoRepository.findById((Long) targetId).orElse(null), perm, authentication);
            case "COMMENT" -> checkComment(commentRepository.findById((Long) targetId).orElse(null), perm, authentication);
            default -> false;
        };
    }

    private boolean checkVideo(Video video, String perm, Authentication auth) {
        if (video == null || auth == null || auth.getPrincipal() == null) {
            return false;
        }
        Object principal = auth.getPrincipal();
        boolean admin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (admin) {
            return true;
        }
        if (!(principal instanceof CustomUserDetails me)) {
            // 匿名用户：只有 read 且视频公开才允许
            return "read".equals(perm) && "PUBLIC".equals(video.getStatus());
        }
        boolean owner = video.getAuthorId() != null && video.getAuthorId().equals(me.getUserId());
        return switch (perm) {
            case "read" -> "PUBLIC".equals(video.getStatus()) || owner;
            case "edit", "delete" -> owner;
            default -> false;
        };
    }

    private boolean checkComment(Comment comment, String perm, Authentication auth) {
        if (comment == null || auth == null) {
            return false;
        }
        boolean admin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (admin) {
            return true;
        }
        if (auth.getPrincipal() instanceof CustomUserDetails me) {
            return comment.getUserId().equals(me.getUserId());
        }
        return false;
    }
}
