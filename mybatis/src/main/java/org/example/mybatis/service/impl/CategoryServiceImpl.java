package org.example.mybatis.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.mybatis.common.BizException;
import org.example.mybatis.entity.Category;
import org.example.mybatis.mapper.CategoryMapper;
import org.example.mybatis.mapper.NovelMapper;
import org.example.mybatis.service.CategoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 分类服务
 *
 * <p>这里可以观察到【二级缓存】的效果：
 * CategoryMapper 上有 {@code @CacheNamespace}，第一次查会落数据库，
 * 之后命中缓存不再打印 SQL；执行了新增/修改/删除（写操作）后缓存被清空。
 */
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;
    private final NovelMapper novelMapper;

    @Override
    public List<Category> listAll() {
        return categoryMapper.selectAll();
    }

    @Override
    public List<Category> listWithNovels() {
        return categoryMapper.selectWithNovels();
    }

    @Override
    public List<Category> listNonEmpty() {
        return categoryMapper.selectNonEmpty();
    }

    @Override
    public Category getById(Long id) {
        return categoryMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(Category category) {
        if (!StringUtils.hasText(category.getName())) {
            throw new BizException("分类名不能为空");
        }
        if (categoryMapper.countByName(null, category.getName().trim()) > 0) {
            throw new BizException("分类已存在：" + category.getName());
        }
        category.setName(category.getName().trim());
        if (category.getSortNum() == null) {
            category.setSortNum(0);
        }
        categoryMapper.insert(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Category category) {
        Category db = categoryMapper.selectById(category.getId());
        if (db == null) {
            throw new BizException("分类不存在");
        }
        if (StringUtils.hasText(category.getName())
                && categoryMapper.countByName(category.getId(), category.getName().trim()) > 0) {
            throw new BizException("分类名重复");
        }
        categoryMapper.update(category);
    }

    /**
     * 删除分类：该分类下还有小说时不允许删除（多表校验）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        Category db = categoryMapper.selectById(id);
        if (db == null) {
            throw new BizException("分类不存在");
        }
        long used = novelMapper.selectWithNovelCountByCategory(id);
        if (used > 0) {
            throw new BizException("该分类下还有 " + used + " 本小说，不能删除");
        }
        categoryMapper.deleteById(id);
    }

    @Override
    public long count() {
        return categoryMapper.count();
    }
}
