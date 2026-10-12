package org.example.mybatis.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.mybatis.common.BizException;
import org.example.mybatis.common.PageResult;
import org.example.mybatis.dto.CategoryStatVO;
import org.example.mybatis.dto.NovelDetailVO;
import org.example.mybatis.dto.NovelQuery;
import org.example.mybatis.dto.NovelVO;
import org.example.mybatis.entity.Chapter;
import org.example.mybatis.entity.Novel;
import org.example.mybatis.entity.SysLog;
import org.example.mybatis.mapper.ChapterMapper;
import org.example.mybatis.mapper.FavoriteMapper;
import org.example.mybatis.mapper.HistoryMapper;
import org.example.mybatis.mapper.NovelMapper;
import org.example.mybatis.mapper.StatMapper;
import org.example.mybatis.mapper.SysLogMapper;
import org.example.mybatis.plugin.PageContext;
import org.example.mybatis.service.NovelService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class NovelServiceImpl implements NovelService {

    /**
     * 章节标题：支持「第一章」「第1章」「第 1 章」「第一节」「第1回」等多种写法
     * (?m) 开启多行模式，^ 匹配每行开头
     */
    private static final Pattern CHAPTER_PATTERN = Pattern.compile(
            "(?m)^\\s*(第\\s*[0-9零一二三四五六七八九十百千万两]{1,12}\\s*[章节回篇集].{0,60})");

    private final NovelMapper novelMapper;
    private final ChapterMapper chapterMapper;
    private final FavoriteMapper favoriteMapper;
    private final HistoryMapper historyMapper;
    private final StatMapper statMapper;
    private final SysLogMapper sysLogMapper;

    @Value("${novel.upload-dir:./upload/novels/}")
    private String uploadDir;

    // ==================== 查询 ====================

    @Override
    public PageResult<NovelVO> search(NovelQuery query, int pageNum, int pageSize) {
        try {
            PageContext.start(pageNum, pageSize);
            List<NovelVO> list = novelMapper.selectByCondition(query == null ? new NovelQuery() : query);
            long total = PageContext.getTotal();
            return PageResult.of(list, total, pageNum, pageSize);
        } finally {
            PageContext.clear();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NovelDetailVO detail(Long novelId, Long userId) {
        NovelDetailVO vo = novelMapper.selectDetail(novelId, userId);
        if (vo == null) {
            throw new BizException("小说不存在或已下架");
        }
        // 浏览量自增（原子 update，不怕并发丢更新）
        novelMapper.incrViewCount(novelId);
        vo.setViewCount((vo.getViewCount() == null ? 0 : vo.getViewCount()) + 1);
        return vo;
    }

    @Override
    public Novel getById(Long id) {
        return novelMapper.selectById(id);
    }

    @Override
    public List<NovelVO> hotTop(int limit, Long categoryId) {
        return novelMapper.selectHotTop(Math.max(1, limit), categoryId);
    }

    // ==================== 增删改 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(Novel novel) {
        if (!StringUtils.hasText(novel.getTitle())) {
            throw new BizException("书名不能为空");
        }
        novel.setTitle(novel.getTitle().trim());
        if (!StringUtils.hasText(novel.getStatus())) {
            novel.setStatus("SERIALIZING");
        }
        if (novel.getTags() == null) {
            novel.setTags(new ArrayList<>());
        }
        novelMapper.insert(novel);
        writeLog(novel.getId(), "新增小说：" + novel.getTitle());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Novel novel) {
        Novel db = novelMapper.selectById(novel.getId());
        if (db == null) {
            throw new BizException("小说不存在");
        }
        // updateSelective：XML 里用 <set> 只更新非空字段
        novelMapper.updateSelective(novel);
    }

    /**
     * 删除小说：级联删除章节、收藏、阅读历史 —— 全部在一个事务里，
     * 任何一步失败都会整体回滚。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        Novel db = novelMapper.selectById(id);
        if (db == null) {
            throw new BizException("小说不存在");
        }
        chapterMapper.deleteByNovelId(id);
        favoriteMapper.deleteByNovelId(id);
        historyMapper.deleteByNovelId(id);
        novelMapper.logicDelete(id);
        writeLog(id, "删除小说：" + db.getTitle());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchRemove(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BizException("请选择要删除的小说");
        }
        for (Long id : ids) {
            chapterMapper.deleteByNovelId(id);
            favoriteMapper.deleteByNovelId(id);
            historyMapper.deleteByNovelId(id);
        }
        novelMapper.batchLogicDelete(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int addChapter(Chapter chapter) {
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
        String content = chapter.getContent() == null ? "" : chapter.getContent();

        Integer no = chapter.getChapterNo();
        if (no == null) {
            // 没传章节号就追加到末尾
            no = (int) chapterMapper.countByNovelId(chapter.getNovelId()) + 1;
            chapter.setChapterNo(no);
        }
        chapter.setContent(content);
        chapter.setWordCount(content.length());
        chapterMapper.insert(chapter);
        novelMapper.refreshStatByChapter(chapter.getNovelId());
        return no;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refreshStat(Long novelId) {
        novelMapper.refreshStatByChapter(novelId);
    }

    // ==================== 上传 TXT 自动分章 ====================

    /**
     * 上传 TXT -> 保存原文件 -> 解析 -> 保存小说 -> 批量写章节 -> 回写统计
     *
     * <p>整个过程在一个事务里：章节入库失败则小说记录也会回滚。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> upload(MultipartFile file, Novel meta) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BizException("请选择要上传的文件");
        }
        String originalName = StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "novel.txt";

        // 1) 读取文本（自动识别 UTF-8 / GBK）
        String text = readText(file.getInputStream());
        if (!StringUtils.hasText(text)) {
            throw new BizException("文件内容为空");
        }

        // 2) 元数据：文件名没填就用文件名当标题
        Novel novel = new Novel();
        novel.setTitle(StringUtils.hasText(meta.getTitle()) ? meta.getTitle() : stripExt(originalName));
        novel.setAuthor(meta.getAuthor());
        novel.setCategoryId(meta.getCategoryId());
        novel.setCoverUrl(meta.getCoverUrl());
        novel.setDescription(meta.getDescription());
        novel.setStatus(StringUtils.hasText(meta.getStatus()) ? meta.getStatus() : "SERIALIZING");
        novel.setTags(meta.getTags() == null ? new ArrayList<>() : meta.getTags());

        // 3) 落盘原文件
        Path dir = Path.of(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(dir);
        String savedName = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "_" + UUID.randomUUID().toString().substring(0, 8) + ".txt";
        Path target = dir.resolve(savedName);
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
        novel.setFilePath(target.toString());

        // 4) 保存小说（主键回填）
        novelMapper.insert(novel);

        // 5) 解析并批量写入章节（一条 INSERT 写多行）
        List<Chapter> chapters = splitChapters(novel.getId(), text);
        if (chapters.isEmpty()) {
            throw new BizException("未能解析出任何章节，请检查文件格式（需包含「第X章」这样的标题）");
        }
        chapterMapper.batchInsert(chapters);

        // 6) 回写字数/章节数
        novelMapper.refreshStatByChapter(novel.getId());
        writeLog(novel.getId(), "上传小说并自动分章：" + novel.getTitle() + "，共 " + chapters.size() + " 章");

        Map<String, Object> res = new HashMap<>();
        res.put("novelId", novel.getId());
        res.put("title", novel.getTitle());
        res.put("chapterCount", chapters.size());
        res.put("wordCount", chapters.stream().mapToInt(c -> c.getWordCount() == null ? 0 : c.getWordCount()).sum());
        res.put("filePath", novel.getFilePath());
        return res;
    }

    /**
     * 按「第X章」切分全文
     */
    private List<Chapter> splitChapters(Long novelId, String text) {
        List<Chapter> list = new ArrayList<>();

        // 1) 先把所有章节标题的位置找出来
        List<int[]> spans = new ArrayList<>();
        List<String> titles = new ArrayList<>();
        Matcher matcher = CHAPTER_PATTERN.matcher(text);
        while (matcher.find()) {
            spans.add(new int[]{matcher.start(), matcher.end()});
            titles.add(cleanTitle(matcher.group(1)));
        }

        // 2) 一个都没找到：整篇当一章
        if (spans.isEmpty()) {
            list.add(buildChapter(novelId, 1, "全文", text.trim()));
            return list;
        }

        // 3) 第一个标题之前的内容作为「前言」
        if (spans.get(0)[0] > 0) {
            String head = text.substring(0, spans.get(0)[0]).trim();
            if (head.length() >= 10) {
                list.add(buildChapter(novelId, list.size() + 1, "前言", head));
            }
        }

        // 4) 逐章切分：内容 = 当前标题结尾 ~ 下一个标题开头
        for (int i = 0; i < spans.size(); i++) {
            int bodyStart = Math.min(spans.get(i)[1], text.length());
            int bodyEnd = (i + 1 < spans.size()) ? spans.get(i + 1)[0] : text.length();
            String body = (bodyEnd > bodyStart) ? text.substring(bodyStart, bodyEnd) : "";
            String title = titles.get(i);
            // 标题行本身也算内容的一部分？不 —— 标题已单独存储，正文从标题行之后开始
            list.add(buildChapter(novelId, list.size() + 1, title, body));
        }
        return list;
    }

    private Chapter buildChapter(Long novelId, int no, String title, String body) {
        String content = body == null ? "" : body.trim();
        Chapter c = new Chapter();
        c.setNovelId(novelId);
        c.setChapterNo(no);
        c.setTitle(title);
        c.setContent(content);
        c.setWordCount(content.length());
        return c;
    }

    private String cleanTitle(String raw) {
        return raw == null ? "未命名" : raw.replaceAll("\\s+", " ").trim();
    }

    /**
     * 读取文本并尽量识别编码：先按 UTF-8 严格解码，失败再按 GBK
     */
    private String readText(InputStream in) throws IOException {
        byte[] bytes = in.readAllBytes();
        try {
            CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT);
            return decoder.decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException e) {
            log.info("非 UTF-8 编码，改用 GBK 解码");
            return new String(bytes, Charset.forName("GBK"));
        }
    }

    private String stripExt(String fileName) {
        int idx = fileName.lastIndexOf('.');
        return idx > 0 ? fileName.substring(0, idx) : fileName;
    }

    // ==================== 统计报表 ====================

    @Override
    public List<CategoryStatVO> categoryStat() {
        return statMapper.selectCategoryStat();
    }

    @Override
    public Map<String, Object> authorRank(int minCount, int limit) {
        Map<String, Object> res = new HashMap<>();
        res.put("minCount", minCount);
        res.put("list", statMapper.selectAuthorRank(Math.max(1, minCount), Math.max(1, limit)));
        return res;
    }

    @Override
    public Map<String, Object> overview() {
        Map<String, Object> overview = new HashMap<>(statMapper.selectOverview());
        overview.put("categoryStat", statMapper.selectCategoryStat());
        List<Map<String, Object>> statusCount = statMapper.selectAuthorRank(1, 5);
        overview.put("authorTop", statusCount);
        return overview;
    }

    @Override
    public List<Map<String, Object>> dailyNovelCount(int days) {
        return statMapper.selectDailyNovelCount(days <= 0 ? 7 : days);
    }

    @Override
    public List<Map<String, Object>> dailyDownloadCount(int days) {
        return statMapper.selectDailyDownloadCount(days <= 0 ? 7 : days);
    }

    @Override
    public List<Map<String, Object>> userActiveRank(int limit) {
        return statMapper.selectUserActiveRank(limit <= 0 ? 10 : limit);
    }

    /** 写一条操作日志 */
    private void writeLog(Long novelId, String operation) {
        SysLog logEntity = new SysLog();
        logEntity.setUserId(null);
        logEntity.setUsername("system");
        logEntity.setModule("小说管理");
        logEntity.setOperation(operation);
        logEntity.setMethod("novel-" + novelId);
        logEntity.setCostMs(0);
        sysLogMapper.insert(logEntity);
    }
}
