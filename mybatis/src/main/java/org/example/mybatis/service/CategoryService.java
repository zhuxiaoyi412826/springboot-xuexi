package org.example.mybatis.service;

import org.example.mybatis.entity.Category;

import java.util.List;

public interface CategoryService {

    List<Category> listAll();

    /** 分类 + 该分类下的小说（一对多一次查出，XML <collection>） */
    List<Category> listWithNovels();

    /** 只含小说的分类（HAVING 演示） */
    List<Category> listNonEmpty();

    Category getById(Long id);

    void save(Category category);

    void update(Category category);

    void remove(Long id);

    long count();
}
