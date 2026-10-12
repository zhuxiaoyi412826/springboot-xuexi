package org.example.mybatis.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.mybatis.dto.FavoriteVO;

import java.util.List;

/**
 * 收藏 Mapper
 */
public interface FavoriteMapper {

    // ==================== 注解：简单 SQL ====================

    /** 唯一性校验 / 是否已收藏 */
    @Select("SELECT COUNT(*) FROM favorite WHERE user_id = #{userId} AND novel_id = #{novelId}")
    long countByUserAndNovel(@Param("userId") Long userId, @Param("novelId") Long novelId);

    /** INSERT IGNORE：重复收藏不报错，返回影响行数 0 */
    @Insert("INSERT IGNORE INTO favorite (user_id, novel_id) VALUES (#{userId}, #{novelId})")
    int insert(@Param("userId") Long userId, @Param("novelId") Long novelId);

    @Delete("DELETE FROM favorite WHERE user_id = #{userId} AND novel_id = #{novelId}")
    int delete(@Param("userId") Long userId, @Param("novelId") Long novelId);

    @Delete("DELETE FROM favorite WHERE novel_id = #{novelId}")
    int deleteByNovelId(@Param("novelId") Long novelId);

    @Delete("DELETE FROM favorite WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM favorite WHERE novel_id = #{novelId}")
    long countByNovelId(@Param("novelId") Long novelId);

    @Select("SELECT COUNT(*) FROM favorite WHERE user_id = #{userId}")
    long countByUserId(@Param("userId") Long userId);

    /** 批量判断是否收藏（XML foreach 演示） */
    List<Long> selectFavoritedNovelIds(@Param("userId") Long userId, @Param("novelIds") List<Long> novelIds);

    // ==================== XML：多表关联 ====================

    /**
     * 我的收藏列表：收藏表 JOIN 小说表 JOIN 分类表 LEFT JOIN 阅读历史
     */
    List<FavoriteVO> selectMyFavorites(@Param("userId") Long userId);
}
