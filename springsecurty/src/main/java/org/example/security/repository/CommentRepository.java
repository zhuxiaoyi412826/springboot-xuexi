package org.example.security.repository;

import org.example.security.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByVideoIdAndStatusOrderByCreateTimeDesc(Long videoId, Integer status);

    List<Comment> findByUserIdOrderByCreateTimeDesc(Long userId);

    @Query("select c from Comment c where (:kw is null or :kw = '' or c.content like concat('%', :kw, '%'))")
    Page<Comment> searchAdmin(@Param("kw") String kw, Pageable pageable);

    long countByVideoIdAndStatus(Long videoId, Integer status);

    @Modifying
    @Query("update Comment c set c.status = 1 where c.id = :id")
    int softDelete(@Param("id") Long id);
}
