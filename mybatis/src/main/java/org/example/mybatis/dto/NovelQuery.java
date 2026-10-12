package org.example.mybatis.dto;

import lombok.Data;

import java.util.List;

/**
 * 小说多条件查询条件
 *
 * <p>所有字段都可以为空 —— XML 里用 {@code <if>} 判断后再拼接，
 * 做到「传什么条件拼什么条件」。
 */
@Data
public class NovelQuery {

    /** 关键字：标题 / 作者 / 简介模糊匹配 */
    private String keyword;

    /** 分类 ID，null 表示不限 */
    private Long categoryId;

    /** 多个分类（演示 foreach IN 查询） */
    private List<Long> categoryIds;

    /** 作者精确匹配 */
    private String author;

    /** 连载状态：SERIALIZING / FINISHED */
    private String status;

    /** 标签：任意一个命中即可（演示 foreach + LIKE OR） */
    private List<String> tags;

    /** 字数下限 */
    private Long minWordCount;

    /** 字数上限 */
    private Long maxWordCount;

    /** 只看有章节的小说（演示 EXISTS 子查询） */
    private Boolean hasChapter;

    /** 当前用户 ID，用于查询「我是否收藏过」 */
    private Long userId;

    /** 排序：hot 热门 / favorite 收藏多 / word 字数多 / word_desc / new 最新上架 / random 随机 */
    private String orderBy;
}
