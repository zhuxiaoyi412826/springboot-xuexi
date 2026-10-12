package org.example.mybatis.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.mybatis.dto.UserQuery;
import org.example.mybatis.entity.User;

import java.util.List;

/**
 * 用户 Mapper
 *
 * <p>本类演示【注解 + XML 混合】：
 * <ul>
 *   <li>简单单表 CRUD -> 直接写在接口上的 {@code @Select/@Insert/@Update/@Delete}</li>
 *   <li>多条件动态查询、批量操作 -> 写在 resources/mapper/UserMapper.xml</li>
 * </ul>
 *
 * <p>注意：同一个方法不能既写注解又配 XML，否则启动报错（MappedStatement 重复）。
 */
public interface UserMapper {

    // ==================== 注解：简单 CRUD ====================

    @Select("SELECT id, username, password, nickname, avatar, role, status, create_time, update_time, deleted " +
            "FROM sys_user WHERE id = #{id} AND deleted = 0")
    User selectById(@Param("id") Long id);

    @Select("SELECT id, username, password, nickname, avatar, role, status, create_time, update_time, deleted " +
            "FROM sys_user WHERE username = #{username} AND deleted = 0")
    User selectByUsername(@Param("username") String username);

    /**
     * 新增用户，自增主键通过 useGeneratedKeys 回填到 user.id
     */
    @Insert("INSERT INTO sys_user (username, password, nickname, avatar, role, status) " +
            "VALUES (#{username}, #{password}, #{nickname}, #{avatar}, #{role}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    @Update("UPDATE sys_user SET nickname = #{nickname}, avatar = #{avatar}, role = #{role}, status = #{status} " +
            "WHERE id = #{id} AND deleted = 0")
    int update(User user);

    @Update("UPDATE sys_user SET password = #{password} WHERE id = #{id}")
    int updatePassword(@Param("id") Long id, @Param("password") String password);

    @Update("UPDATE sys_user SET status = #{status} WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    /** 逻辑删除 */
    @Update("UPDATE sys_user SET deleted = 1 WHERE id = #{id}")
    int logicDelete(@Param("id") Long id);

    @Delete("DELETE FROM sys_user WHERE id = #{id}")
    int deleteById(@Param("id") Long id);

    /** COUNT 聚合函数 */
    @Select("SELECT COUNT(*) FROM sys_user WHERE deleted = 0")
    long count();

    /** IFNULL 函数：没有数据的表也返回 0 */
    @Select("SELECT IFNULL(SUM(1), 0) FROM sys_user WHERE deleted = 0 AND role = #{role}")
    long countByRole(@Param("role") String role);

    // ==================== XML：复杂语句 ====================

    /**
     * 多条件动态查询（XML 中的 {@code <where> <if> <choose>} 演示）
     * 配合自研分页插件使用。
     */
    List<User> selectByCondition(UserQuery query);

    /**
     * 批量逻辑删除（XML 中的 {@code <foreach>} 演示）
     */
    int batchLogicDelete(@Param("ids") List<Long> ids);

    /**
     * 批量查询（XML 中的 {@code <foreach>} 构造 IN 条件）
     */
    List<User> selectByIds(@Param("ids") List<Long> ids);
}
