package org.example.mybatis.plugin;

import lombok.Data;

/**
 * 分页上下文（ThreadLocal）
 *
 * <p>配合 {@link PageInterceptor} 使用：
 * <pre>
 *     PageContext.start(1, 10);          // 告诉拦截器要分页
 *     List&lt;Novel&gt; list = mapper.selectXxx(..);
 *     long total = PageContext.getTotal();
 *     PageContext.clear();
 * </pre>
 *
 * <p>原理和 PageHelper 一致：分页参数通过 ThreadLocal 传递给拦截器，
 * 拦截器再改写 SQL 追加 LIMIT。ThreadLocal 保证了多线程之间互不干扰。
 */
public final class PageContext {

    private static final ThreadLocal<Page> TL = new ThreadLocal<>();

    private PageContext() {
    }

    /** 开启分页。调用后最近的一次查询会被自动分页 */
    public static void start(int pageNum, int pageSize) {
        TL.set(new Page(pageNum, pageSize));
    }

    /** 当前分页信息，未开启分页时返回 null */
    public static Page get() {
        return TL.get();
    }

    /** 取查询到的总记录数 */
    public static long getTotal() {
        Page page = TL.get();
        return page == null ? 0 : page.getTotal();
    }

    /** 用完必须清理，避免线程池环境下串页 */
    public static void clear() {
        TL.remove();
    }

    @Data
    public static class Page {
        private final int pageNum;
        private final int pageSize;
        private long total;

        public Page(int pageNum, int pageSize) {
            this.pageNum = Math.max(pageNum, 1);
            this.pageSize = Math.max(pageSize, 1);
        }

        /** MySQL LIMIT 偏移量 */
        public int getOffset() {
            return (pageNum - 1) * pageSize;
        }
    }
}
