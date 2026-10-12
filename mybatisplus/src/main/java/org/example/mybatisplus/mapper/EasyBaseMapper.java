package org.example.mybatisplus.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

/**
 * 增强版 BaseMapper：加上自定义注入的批量插入方法。
 *
 * <p>方法名 insertBatchSomeColumn 必须与 {@code InsertBatchSomeColumn} 注入的 SQL 名一致。
 * 继承它之后，对应的 Mapper 就自动拥有 insertBatchSomeColumn 能力。
 */
public interface EasyBaseMapper<T> extends BaseMapper<T> {

    /**
     * 一条 SQL 批量插入（INSERT INTO ... VALUES (),(),()）
     *
     * @param list 实体列表
     * @return 影响行数
     */
    int insertBatchSomeColumn(List<T> list);
}
