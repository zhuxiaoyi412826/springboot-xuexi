package org.example.security.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.security.common.BizException;
import org.example.security.common.Result;
import org.example.security.dto.PageResult;
import org.example.security.dto.VideoVO;
import org.example.security.entity.Favorite;
import org.example.security.entity.Video;
import org.example.security.repository.FavoriteRepository;
import org.example.security.repository.VideoRepository;
import org.example.security.security.CustomUserDetails;
import org.example.security.security.PermissionChecker;
import org.example.security.security.SecurityUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 视频业务。
 *
 * <p>这里集中演示了三种授权：
 * <ul>
 *     <li>数据权限：{@link #list} 中匿名只看公开，登录用户看「公开+自己」，管理员看全部</li>
 *     <li>方法级权限：{@code @PreAuthorize} / {@code @PostFilter}</li>
 *     <li>对象级权限：{@code @perm.canEditVideo(#id, authentication)} 或 hasPermission(...)</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VideoService {

    private final VideoRepository videoRepository;
    private final FavoriteRepository favoriteRepository;
    private final PermissionChecker permissionChecker;

    /** 列表：数据权限过滤 */
    public PageResult<VideoVO> list(String keyword, String category, String sort, int page, int size) {
        Sort sortObj = "hot".equalsIgnoreCase(sort)
                ? Sort.by(Sort.Direction.DESC, "viewCount")
                : Sort.by(Sort.Direction.DESC, "createTime");
        PageRequest pageable = PageRequest.of(Math.max(page - 1, 0), Math.min(size, 50), sortObj);

        Page<Video> videos;
        Long uid = SecurityUtil.currentUserId();
        if (SecurityUtil.isAdmin()) {
            // 管理员：看全部（含待审核、私有）
            videos = videoRepository.searchAdmin(keyword, category, null, pageable);
        } else if (uid != null) {
            // 普通用户：公开 + 自己上传的
            videos = videoRepository.searchVisible(uid, keyword, category, pageable);
        } else {
            // 匿名：只看公开
            videos = videoRepository.searchPublic(keyword, category, pageable);
        }

        Set<Long> favorited = favoritedIds(uid);
        List<VideoVO> list = videos.getContent().stream().map(v -> {
            VideoVO vo = VideoVO.from(v);
            vo.setFavorited(uid != null && favorited.contains(v.getId()));
            vo.setCanEdit(permissionChecker.canEditVideo(v.getId(), SecurityUtil.auth()));
            return vo;
        }).toList();

        return PageResult.of(list, videos.getTotalElements(), page, size);
    }

    public List<VideoVO> hot() {
        return videoRepository.findTop10ByStatusOrderByViewCountDesc("PUBLIC")
                .stream().map(VideoVO::from).toList();
    }

    /** 详情：非公开视频只有作者/管理员能看（对象级权限） */
    @Transactional
    public VideoVO detail(Long id) {
        Video video = videoRepository.findById(id).orElseThrow(() -> new BizException("视频不存在"));
        if (!"PUBLIC".equals(video.getStatus()) && !permissionChecker.canEditVideo(id, SecurityUtil.auth())) {
            throw new BizException(403, "该视频未公开，无权查看");
        }
        videoRepository.incrView(id);
        VideoVO vo = VideoVO.from(video);
        vo.setFavorited(isFavorited(id));
        vo.setCanEdit(permissionChecker.canEditVideo(id, SecurityUtil.auth()));
        return vo;
    }

    @Transactional
    public void like(Long id) {
        videoRepository.incrLike(id);
    }

    /** 收藏/取消收藏，返回收藏后的状态 */
    @Transactional
    public boolean toggleFavorite(Long id) {
        Long uid = SecurityUtil.currentUserId();
        if (uid == null) {
            throw new BizException(401, "请先登录");
        }
        var exist = favoriteRepository.findByUserIdAndVideoId(uid, id);
        if (exist.isPresent()) {
            favoriteRepository.deleteByUserIdAndVideoId(uid, id);
            return false;
        }
        Favorite favorite = new Favorite();
        favorite.setUserId(uid);
        favorite.setVideoId(id);
        favorite.setCreateTime(LocalDateTime.now());
        favoriteRepository.save(favorite);
        return true;
    }

    /** 上传/新增视频：作者 = 当前登录用户 */
    @Transactional
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public Video create(Video video) {
        Long uid = SecurityUtil.currentUserId();
        Authentication auth = SecurityUtil.auth();
        video.setAuthorId(uid);
        video.setAuthorName(auth.getPrincipal() instanceof CustomUserDetails u ? u.getNickname() : auth.getName());
        video.setCreateTime(LocalDateTime.now());
        video.setViewCount(0L);
        video.setLikeCount(0L);
        if (video.getStatus() == null || video.getStatus().isBlank()) {
            video.setStatus("PUBLIC");
        }
        return videoRepository.save(video);
    }

    /** 修改：只有作者本人或管理员（对象级权限 + 方法级权限） */
    @Transactional
    @PreAuthorize("@perm.canEditVideo(#id, authentication)")
    public Video update(Long id, Video patch) {
        Video video = videoRepository.findById(id).orElseThrow(() -> new BizException("视频不存在"));
        if (patch.getTitle() != null) {
            video.setTitle(patch.getTitle());
        }
        if (patch.getDescription() != null) {
            video.setDescription(patch.getDescription());
        }
        if (patch.getCoverUrl() != null) {
            video.setCoverUrl(patch.getCoverUrl());
        }
        if (patch.getVideoUrl() != null) {
            video.setVideoUrl(patch.getVideoUrl());
        }
        if (patch.getCategory() != null) {
            video.setCategory(patch.getCategory());
        }
        if (patch.getTags() != null) {
            video.setTags(patch.getTags());
        }
        if (patch.getDurationSec() != null) {
            video.setDurationSec(patch.getDurationSec());
        }
        return videoRepository.save(video);
    }

    /** 删除：对象级权限的另一种写法（走自定义 PermissionEvaluator） */
    @Transactional
    @PreAuthorize("hasPermission(#id, 'VIDEO', 'delete')")
    public void delete(Long id) {
        videoRepository.deleteById(id);
    }

    /** 审核：只有管理员（角色级权限） */
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void changeStatus(Long id, String status) {
        Video video = videoRepository.findById(id).orElseThrow(() -> new BizException("视频不存在"));
        video.setStatus(status);
        videoRepository.save(video);
    }

    /**
     * 「我的视频」——演示 @PostFilter：先查全部，再按权限过滤结果。
     * 管理员返回全部，普通用户只剩自己上传的。
     */
    @PostFilter("hasRole('ADMIN') or @perm.canEditVideo(filterObject.id, authentication)")
    public List<Video> listMineRaw() {
        return videoRepository.findAll();
    }

    public List<VideoVO> mine() {
        Long uid = SecurityUtil.currentUserId();
        if (uid == null) {
            return List.of();
        }
        return videoRepository.findByAuthor(uid).stream().map(v -> {
            VideoVO vo = VideoVO.from(v);
            vo.setCanEdit(true);
            return vo;
        }).toList();
    }

    private Set<Long> favoritedIds(Long uid) {
        Set<Long> ids = new HashSet<>();
        if (uid != null) {
            favoriteRepository.findByUserIdOrderByIdDesc(uid).forEach(f -> ids.add(f.getVideoId()));
        }
        return ids;
    }

    private boolean isFavorited(Long videoId) {
        Long uid = SecurityUtil.currentUserId();
        return uid != null && favoriteRepository.findByUserIdAndVideoId(uid, videoId).isPresent();
    }

    /** 占位：Result 类型仅用于演示返回值，可忽略 */
    public Result<String> placeholder() {
        return Result.ok("ok");
    }
}
