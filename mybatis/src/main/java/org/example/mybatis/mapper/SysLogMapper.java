package org.example.mybatis.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.mybatis.entity.SysLog;

import java.util.List;

/**
 * 操作日志 Mapper（纯注解，最简单的一档）
 */
public interface SysLogMapper {

    @Insert("INSERT INTO sys_log (user_id, username, module, operation, method, cost_ms) " +
            "VALUES (#{userId}, #{username}, #{module}, #{operation}, #{method}, #{costMs})")
    int insert(SysLog log);

    /** 配合自研分页插件使用：无需写 LIMIT，拦截器自动追加 */
    @Select("SELECT id, user_id, username, module, operation, method, cost_ms, create_time " +
            "FROM sys_log ORDER BY id DESC")
    List<SysLog> selectRecent();

    @Select("SELECT COUNT(*) FROM sys_log")
    long count();

    @Select("SELECT IFNULL(AVG(cost_ms), 0) FROM sys_log WHERE module = #{module}")
    long avgCostByModule(@Param("module") String module);
}
