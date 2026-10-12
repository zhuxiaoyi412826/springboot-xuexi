package org.example.security.repository;

import org.example.security.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    List<Favorite> findByUserIdOrderByIdDesc(Long userId);

    Optional<Favorite> findByUserIdAndVideoId(Long userId, Long videoId);

    @Modifying
    int deleteByUserIdAndVideoId(Long userId, Long videoId);

    long countByVideoId(Long videoId);

    long countByUserId(Long userId);
}
