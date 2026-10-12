package org.example.mybatis.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.mybatis.common.BizException;
import org.example.mybatis.common.PageResult;
import org.example.mybatis.dto.FavoriteVO;
import org.example.mybatis.entity.Novel;
import org.example.mybatis.mapper.FavoriteMapper;
import org.example.mybatis.mapper.NovelMapper;
import org.example.mybatis.plugin.PageContext;
import org.example.mybatis.service.FavoriteService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 收藏服务
 *
 * <p>收藏要同时改两张表（favorite 插入/删除 + novel.favorite_count 计数），
 * 这就是典型的「事务」场景：要么都成功，要么都回滚，否则会出现计数对不上的脏数据。
 */
@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {

    private final FavoriteMapper favoriteMapper;
    private final NovelMapper novelMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean toggle(Long userId, Long novelId) {
        Novel novel = novelMapper.selectById(novelId);
        if (novel == null) {
            throw new BizException("小说不存在");
        }
        long existed = favoriteMapper.countByUserAndNovel(userId, novelId);
        if (existed > 0) {
            favoriteMapper.delete(userId, novelId);
            novelMapper.changeFavoriteCount(novelId, -1);
            return false;
        }
        favoriteMapper.insert(userId, novelId);
        novelMapper.changeFavoriteCount(novelId, 1);
        return true;
    }

    @Override
    public boolean isFavorited(Long userId, Long novelId) {
        if (userId == null) {
            return false;
        }
        return favoriteMapper.countByUserAndNovel(userId, novelId) > 0;
    }

    @Override
    public PageResult<FavoriteVO> myFavorites(Long userId, int pageNum, int pageSize) {
        try {
            PageContext.start(pageNum, pageSize);
            List<FavoriteVO> list = favoriteMapper.selectMyFavorites(userId);
            long total = PageContext.getTotal();
            return PageResult.of(list, total, pageNum, pageSize);
        } finally {
            PageContext.clear();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long userId, Long novelId) {
        if (favoriteMapper.delete(userId, novelId) > 0) {
            novelMapper.changeFavoriteCount(novelId, -1);
        }
    }

    @Override
    public long count(Long userId) {
        return favoriteMapper.countByUserId(userId);
    }
}
