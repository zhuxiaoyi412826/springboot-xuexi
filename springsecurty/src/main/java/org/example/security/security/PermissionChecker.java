package org.example.security.security;

import lombok.RequiredArgsConstructor;
import org.example.security.entity.Comment;
import org.example.security.entity.Video;
import org.example.security.repository.CommentRepository;
import org.example.security.repository.VideoRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * SpEL 表达式里可直接引用的 Bean：{@code @PreAuthorize("@perm.canEditVideo(#id, authentication)")}。
 *
 * <p>这是对象级权限的另一种更直观写法（不必实现 PermissionEvaluator）。</p>
 */
@Component("perm")
@RequiredArgsConstructor
public class PermissionChecker {

    private final VideoRepository videoRepository;
    private final CommentRepository commentRepository;

    /** 只有作者本人或管理员能编辑/删除视频 */
    public boolean canEditVideo(Long videoId, Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) {
            return false;
        }
        if (isAdmin(auth)) {
            return true;
        }
        Video video = videoRepository.findById(videoId).orElse(null);
        if (video == null || !(auth.getPrincipal() instanceof CustomUserDetails me)) {
            return false;
        }
        return video.getAuthorId() != null && video.getAuthorId().equals(me.getUserId());
    }

    /** 只有评论作者本人或管理员能删除评论 */
    public boolean canDeleteComment(Long commentId, Authentication auth) {
        if (isAdmin(auth)) {
            return true;
        }
        Comment comment = commentRepository.findById(commentId).orElse(null);
        if (comment == null || !(auth.getPrincipal() instanceof CustomUserDetails me)) {
            return false;
        }
        return comment.getUserId().equals(me.getUserId());
    }

    /** 是否管理员 */
    public boolean isAdmin(Authentication auth) {
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    /** 是否本人（数据权限：用户只能看自己的数据） */
    public boolean isSelf(Long userId, Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof CustomUserDetails me)) {
            return false;
        }
        return me.getUserId().equals(userId);
    }
}
