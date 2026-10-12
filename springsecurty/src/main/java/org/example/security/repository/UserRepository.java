package org.example.security.repository;

import org.example.security.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    /** 管理端：按关键字搜索（用户名/昵称） */
    @Query("select u from User u where (:kw is null or :kw = '' or lower(u.username) like lower(concat('%', :kw, '%')) "
            + "or lower(coalesce(u.nickname,'')) like lower(concat('%', :kw, '%')))")
    List<User> search(@Param("kw") String kw, org.springframework.data.domain.Pageable pageable);

    @Query("select count(u) from User u where (:kw is null or :kw = '' or lower(u.username) like lower(concat('%', :kw, '%')) "
            + "or lower(coalesce(u.nickname,'')) like lower(concat('%', :kw, '%')))")
    long countByKeyword(@Param("kw") String kw);

    @Modifying
    @Query("update User u set u.loginFailCount = 0, u.accountNonLocked = true, u.lockedUntil = null where u.username = :username")
    int resetLock(@Param("username") String username);

    long countBySource(String source);
}
