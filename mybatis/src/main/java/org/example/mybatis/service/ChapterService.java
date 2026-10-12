package org.example.mybatis.service;

import org.example.mybatis.common.PageResult;
import org.example.mybatis.dto.ChapterQuery;
import org.example.mybatis.entity.Chapter;

import java.util.List;

public interface ChapterService {

    /** 全部目录（不分页，阅读器左侧用） */
    List<Chapter> catalog(Long novelId);

    /** 分页目录（后台管理用，演示分页插件） */
    PageResult<Chapter> catalogPage(Long novelId, int pageNum, int pageSize);

    /** 动态条件查询章节 */
    List<Chapter> search(ChapterQuery query);

    /** 读某一章（含正文） */
    Chapter read(Long chapterId);

    /** 按序号读某一章 */
    Chapter readByNo(Long novelId, Integer chapterNo);

    /** 上一章 / 下一章 */
    Chapter prev(Long novelId, Integer chapterNo);

    Chapter next(Long novelId, Integer chapterNo);

    Integer add(Chapter chapter);

    void update(Chapter chapter);

    void remove(Long id);

    long count(Long novelId);
}
