package org.example.security.repository;

import org.example.security.entity.LoginLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LoginLogRepository extends JpaRepository<LoginLog, Long> {

    List<LoginLog> findTop20ByUsernameOrderByIdDesc(String username);

    @Query("select l from LoginLog l where (:kw is null or :kw = '' or l.username like concat('%', :kw, '%'))")
    Page<LoginLog> searchAdmin(String kw, Pageable pageable);
}
