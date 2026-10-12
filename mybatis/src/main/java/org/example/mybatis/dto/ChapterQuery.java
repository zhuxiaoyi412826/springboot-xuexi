package org.example.mybatis.dto;

import lombok.Data;

import java.util.List;

/**
 * 章节查询条件
 */
@Data
public class ChapterQuery {

    private Long novelId;

    /** 章节序号精确查询 */
    private Integer chapterNo;

    /** 标题模糊查询 */
    private String title;

    /** 批量章节号 */
    private List<Integer> chapterNos;

    /** 只查 __ <= __ 序号之前的（翻页游标） */
    private Integer maxChapterNo;

    private String orderBy;
}
