package org.example.mybatis.service;

import org.example.mybatis.common.PageResult;
import org.example.mybatis.dto.CategoryStatVO;
import org.example.mybatis.dto.NovelDetailVO;
import org.example.mybatis.dto.NovelQuery;
import org.example.mybatis.dto.NovelVO;
import org.example.mybatis.entity.Chapter;
import org.example.mybatis.entity.Novel;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface NovelService {

    /** 多条件分页检索 */
    PageResult<NovelVO> search(NovelQuery query, int pageNum, int pageSize);

    /** 详情（会同时累加浏览量） */
    NovelDetailVO detail(Long novelId, Long userId);

    Novel getById(Long id);

    List<NovelVO> hotTop(int limit, Long categoryId);

    void save(Novel novel);

    void update(Novel novel);

    /** 删除小说：级联清理章节/收藏/历史（事务） */
    void remove(Long id);

    void batchRemove(List<Long> ids);

    /**
     * 上传 TXT 并自动分章（事务）
     *
     * @param file   TXT 文件
     * @param meta   书名/作者/分类等信息
     * @return 解析出的章节数
     */
    Map<String, Object> upload(MultipartFile file, Novel meta) throws IOException;

    /** 从已有文本创建章节（手动录入/在线编辑） */
    int addChapter(Chapter chapter);

    /** 上传后手动重算小说的章节数与字数 */
    void refreshStat(Long novelId);

    // ---------------- 统计 ----------------

    List<CategoryStatVO> categoryStat();

    Map<String, Object> authorRank(int minCount, int limit);

    Map<String, Object> overview();

    List<Map<String, Object>> dailyNovelCount(int days);

    List<Map<String, Object>> dailyDownloadCount(int days);

    List<Map<String, Object>> userActiveRank(int limit);
}
