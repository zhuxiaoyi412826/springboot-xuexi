package org.example.mybatisplus.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.spring.service.IService;
import org.example.mybatisplus.dto.BikeQuery;
import org.example.mybatisplus.dto.BikeVO;
import org.example.mybatisplus.dto.CategoryStatVO;
import org.example.mybatisplus.entity.Bike;

import java.util.List;
import java.util.Map;

public interface BikeService extends IService<Bike> {

    /**
     * 卖场分页查询（只看在售商品）
     */
    IPage<Bike> pageForSale(long current, long size, String keyword, String category);

    /**
     * 后台分页查询（含下架商品）
     */
    IPage<Bike> pageForAdmin(long current, long size, String keyword, String category);

    /**
     * 动态 SQL 分页检索商品（带销量）。
     *
     * @param useAnnotation true = 走接口注解实现，false = 走 Mapper XML 实现
     */
    IPage<BikeVO> search(BikeQuery query, long current, long size, boolean useAnnotation);

    /**
     * 动态 SQL 按分类聚合统计。
     *
     * @param useAnnotation true = 走接口注解实现，false = 走 Mapper XML 实现
     */
    List<CategoryStatVO> stat(BikeQuery query, boolean useAnnotation);

    /**
     * 高级检索下拉框选项：所有分类、所有品牌（去重）
     */
    Map<String, List<String>> options();

    /**
     * 批量导入商品（后台「批量上架」业务）。
     *
     * <p>用的是自定义注入的 insertBatchSomeColumn：一条 INSERT 多组 VALUES，
     * 比 saveBatch（JDBC Batch）和逐条 insert 快很多，实验室页面里有耗时对比。
     *
     * @return 实际写入行数
     */
    int importBatch(List<Bike> bikes);
}
