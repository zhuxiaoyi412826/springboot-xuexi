package org.example.security.repository;

import org.example.security.entity.Video;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VideoRepository extends JpaRepository<Video, Long> {

    /** 前台：只看公开视频，可按关键字/分类过滤 */
    @Query("select v from Video v where v.status = 'PUBLIC' "
            + "and (:kw is null or :kw = '' or lower(v.title) like lower(concat('%', :kw, '%')) "
            + "or lower(coalesce(v.description,'')) like lower(concat('%', :kw, '%'))) "
            + "and (:cat is null or :cat = '' or v.category = :cat)")
    Page<Video> searchPublic(@Param("kw") String kw, @Param("cat") String cat, Pageable pageable);

    /** 数据权限：登录用户能看到「公开视频 + 自己上传的视频」 */
    @Query("select v from Video v where (v.status = 'PUBLIC' or v.authorId = :uid) "
            + "and (:kw is null or :kw = '' or lower(v.title) like lower(concat('%', :kw, '%'))) "
            + "and (:cat is null or :cat = '' or v.category = :cat)")
    Page<Video> searchVisible(@Param("uid") Long uid, @Param("kw") String kw,
                              @Param("cat") String cat, Pageable pageable);

    /** 管理端：不限制状态 */
    @Query("select v from Video v where "
            + "(:kw is null or :kw = '' or lower(v.title) like lower(concat('%', :kw, '%'))) "
            + "and (:cat is null or :cat = '' or v.category = :cat) "
            + "and (:status is null or :status = '' or v.status = :status)")
    Page<Video> searchAdmin(@Param("kw") String kw, @Param("cat") String cat,
                            @Param("status") String status, Pageable pageable);

    long countByStatus(String status);

    List<Video> findTop10ByStatusOrderByViewCountDesc(String status);

    @Query("select v from Video v where v.authorId = :authorId order by v.createTime desc")
    List<Video> findByAuthor(@Param("authorId") Long authorId);

    @Modifying
    @Query("update Video v set v.viewCount = v.viewCount + 1 where v.id = :id")
    int incrView(@Param("id") Long id);

    @Modifying
    @Query("update Video v set v.likeCount = v.likeCount + 1 where v.id = :id")
    int incrLike(@Param("id") Long id);

    @Modifying
    @Query("update Video v set v.likeCount = v.likeCount - 1 where v.id = :id and v.likeCount > 0")
    int decrLike(@Param("id") Long id);
}
