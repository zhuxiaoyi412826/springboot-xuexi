package org.example.redis.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.function.Function;

/** 分页结果。 */
@Data
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<T> list;
    private long total;
    private long page;
    private long size;

    public static <E, T> PageResult<T> of(IPage<E> page, List<T> list) {
        PageResult<T> r = new PageResult<>();
        r.setList(list);
        r.setTotal(page.getTotal());
        r.setPage(page.getCurrent());
        r.setSize(page.getSize());
        return r;
    }

    public static <E, T> PageResult<T> of(IPage<E> page, Function<E, T> converter) {
        return of(page, page.getRecords().stream().map(converter).toList());
    }
}
