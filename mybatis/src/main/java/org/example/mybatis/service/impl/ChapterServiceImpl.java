package org.example.mybatis.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.mybatis.common.BizException;
import org.example.mybatis.common.PageResult;
import org.example.mybatis.dto.ChapterQuery;
import org.example.mybatis.entity.Chapter;
import org.example.mybatis.entity.Novel;
import org.example.mybatis.mapper.ChapterMapper;
import org.example.mybatis.mapper.NovelMapper;
import org.example.mybatis.plugin.PageContext;
import org.example.mybatis.service.ChapterService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChapterServiceImpl implements ChapterService {

    private final ChapterMapper chapterMapper;
    private final NovelMapper novelMapper;

    @Override
    public List<Chapter> catalog(Long novelId) {
        return chapterMapper.selectCatalogByNovelId(novelId);
    }

    @Override
    public PageResult<Chapter> catalogPage(Long novelId, int pageNum, int pageSize) {
        try {
            PageContext.start(pageNum, pageSize);
            ChapterQuery query = new ChapterQuery();
            query.setNovelId(novelId);
            query.setOrderBy("asc");
            List<Chapter> list = chapterMapper.selectByCondition(query);
            long total = PageContext.getTotal();
            return PageResult.of(list, total, pageNum, pageSize);
        } finally {
            PageContext.clear();
        }
    }

    @Override
    public List<Chapter> search(ChapterQuery query) {
        return chapterMapper.selectByCondition(query);
    }

    @Override
    public Chapter read(Long chapterId) {
        Chapter chapter = chapterMapper.selectById(chapterId);
        if (chapter == null) {
            throw new BizException("章节不存在");
        }
        return chapter;
    }

    @Override
    public Chapter readByNo(Long novelId, Integer chapterNo) {
        Chapter chapter = chapterMapper.selectByNo(novelId, chapterNo);
        if (chapter == null) {
            throw new BizException("章节不存在");
        }
        return chapter;
    }

    @Override
    public Chapter prev(Long novelId, Integer chapterNo) {
        ChapterQuery query = new ChapterQuery();
        query.setNovelId(novelId);
        query.setMaxChapterNo(chapterNo - 1);
        query.setOrderBy("asc");
        List<Chapter> list = chapterMapper.selectByCondition(query);
        // 取最接近当前章的上一章
        return list.isEmpty() ? null : list.get(list.size() - 1);
    }

    @Override
    public Chapter next(Long novelId, Integer chapterNo) {
        ChapterQuery query = new ChapterQuery();
        query.setNovelId(novelId);
        query.setOrderBy("asc");
        List<Chapter> list = chapterMapper.selectByCondition(query);
        for (Chapter c : list) {
            if (c.getChapterNo() != null && c.getChapterNo() > chapterNo) {
                return c;
            }
        }
        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer add(Chapter chapter) {
        if (chapter.getNovelId() == null) {
            throw new BizException("缺少小说ID");
        }
        Novel novel = novelMapper.selectById(chapter.getNovelId());
        if (novel == null) {
            throw new BizException("小说不存在");
        }
        if (!StringUtils.hasText(chapter.getTitle())) {
            throw new BizException("章节标题不能为空");
        }
        if (chapter.getContent() == null) {
            chapter.setContent("");
        }
        if (chapter.getChapterNo() == null) {
            chapter.setChapterNo((int) chapterMapper.countByNovelId(chapter.getNovelId()) + 1);
        }
        chapter.setWordCount(chapter.getContent().length());
        chapterMapper.insert(chapter);
        // 回写小说的章节数与总字数
        novelMapper.refreshStatByChapter(chapter.getNovelId());
        return chapter.getChapterNo();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Chapter chapter) {
        Chapter db = chapterMapper.selectById(chapter.getId());
        if (db == null) {
            throw new BizException("章节不存在");
        }
        if (chapter.getContent() != null) {
            chapter.setWordCount(chapter.getContent().length());
        }
        chapterMapper.update(chapter);
        novelMapper.refreshStatByChapter(db.getNovelId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        Chapter db = chapterMapper.selectById(id);
        if (db == null) {
            throw new BizException("章节不存在");
        }
        chapterMapper.deleteById(id);
        novelMapper.refreshStatByChapter(db.getNovelId());
    }

    @Override
    public long count(Long novelId) {
        return chapterMapper.countByNovelId(novelId);
    }
}
