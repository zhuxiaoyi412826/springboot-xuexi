package org.example.mybatis.service;

import org.example.mybatis.common.PageResult;
import org.example.mybatis.dto.FavoriteVO;

public interface FavoriteService {

    /** 收藏 / 取消收藏（返回操作后是否已收藏） */
    boolean toggle(Long userId, Long novelId);

    boolean isFavorited(Long userId, Long novelId);

    PageResult<FavoriteVO> myFavorites(Long userId, int pageNum, int pageSize);

    void cancel(Long userId, Long novelId);

    long count(Long userId);
}
