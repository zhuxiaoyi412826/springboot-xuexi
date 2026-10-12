package org.example.mybatisplus.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.servlet.http.HttpSession;
import org.example.mybatisplus.common.Result;
import org.example.mybatisplus.common.SessionUtil;
import org.example.mybatisplus.dto.BikeQuery;
import org.example.mybatisplus.dto.BikeVO;
import org.example.mybatisplus.dto.CategoryStatVO;
import org.example.mybatisplus.entity.Bike;
import org.example.mybatisplus.entity.User;
import org.example.mybatisplus.service.BikeService;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 自行车商品：卖场浏览（所有人）+ 后台 CRUD（仅管理员）
 */
@RestController
@RequestMapping("/api/bikes")
public class BikeController {

    private final BikeService bikeService;

    public BikeController(BikeService bikeService) {
        this.bikeService = bikeService;
    }

    /** 卖场：分页查询在售商品 */
    @GetMapping
    public Result<IPage<Bike>> listForSale(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "8") long size) {
        return Result.ok(bikeService.pageForSale(current, size, keyword, category));
    }

    /** 后台：分页查询全部商品（含下架） */
    @GetMapping("/admin")
    public Result<IPage<Bike>> listForAdmin(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            HttpSession session) {
        SessionUtil.requireAdmin(session);
        return Result.ok(bikeService.pageForAdmin(current, size, keyword, category));
    }

    // ================== 动态 SQL 检索（XML / 注解两种实现） ==================

    /**
     * 高级检索（动态拼接 SQL + 关联订单统计销量）
     *
     * <p>impl=xml 走 Mapper XML，impl=annotation 走接口 @Select 注解，两者返回结果一致。
     */
    @GetMapping("/search")
    public Result<IPage<BikeVO>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String categories,
            @RequestParam(required = false) String brands,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Integer minStock,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer minSold,
            @RequestParam(required = false, defaultValue = "id_desc") String orderBy,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "8") long size,
            @RequestParam(required = false, defaultValue = "xml") String impl,
            HttpSession session) {

        BikeQuery query = new BikeQuery();
        query.setKeyword(keyword);
        query.setCategories(split(categories));
        query.setBrands(split(brands));
        query.setMinPrice(minPrice);
        query.setMaxPrice(maxPrice);
        query.setMinStock(minStock);
        query.setMinSold(minSold);
        query.setOrderBy(orderBy);

        // 未登录或非管理员，强制只看在售商品，避免卖场看到下架商品
        User loginUser = SessionUtil.getLoginUser(session);
        if (loginUser != null && loginUser.isAdmin() && status != null) {
            query.setStatus(status);
        } else {
            query.setStatus(1);
        }

        boolean useAnnotation = "annotation".equalsIgnoreCase(impl);
        return Result.ok(bikeService.search(query, current, size, useAnnotation));
    }

    /**
     * 按分类聚合统计（动态 SQL + GROUP BY），仅管理员
     */
    @GetMapping("/stat")
    public Result<List<CategoryStatVO>> stat(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String categories,
            @RequestParam(required = false) String brands,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Integer minStock,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer minSold,
            @RequestParam(required = false, defaultValue = "sold_desc") String orderBy,
            @RequestParam(required = false, defaultValue = "xml") String impl,
            HttpSession session) {

        SessionUtil.requireAdmin(session);
        BikeQuery query = new BikeQuery();
        query.setKeyword(keyword);
        query.setCategories(split(categories));
        query.setBrands(split(brands));
        query.setMinPrice(minPrice);
        query.setMaxPrice(maxPrice);
        query.setMinStock(minStock);
        query.setMinSold(minSold);
        query.setStatus(status);
        query.setOrderBy(orderBy);

        boolean useAnnotation = "annotation".equalsIgnoreCase(impl);
        return Result.ok(bikeService.stat(query, useAnnotation));
    }

    /**
     * 高级检索下拉框选项（分类 / 品牌去重列表）
     */
    @GetMapping("/options")
    public Result<java.util.Map<String, List<String>>> options() {
        return Result.ok(bikeService.options());
    }

    /** 逗号分隔字符串转 List */
    private List<String> split(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

    /** 商品详情 */
    @GetMapping("/{id}")
    public Result<Bike> detail(@PathVariable Long id) {
        Bike bike = bikeService.getById(id);
        if (bike == null) {
            return Result.fail("商品不存在");
        }
        return Result.ok(bike);
    }

    /** 新增商品（管理员） */
    @PostMapping
    public Result<Bike> save(@RequestBody Bike bike, HttpSession session) {
        SessionUtil.requireAdmin(session);
        bike.setId(null);
        bikeService.saveOrUpdate(bike);
        return Result.ok(bike);
    }

    /** 修改商品（管理员） */
    @PutMapping("/{id}")
    public Result<Bike> update(@PathVariable Long id, @RequestBody Bike bike, HttpSession session) {
        SessionUtil.requireAdmin(session);
        bike.setId(id);
        bikeService.saveOrUpdate(bike);
        return Result.ok(bike);
    }

    /** 删除商品（管理员，逻辑删除） */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpSession session) {
        SessionUtil.requireAdmin(session);
        bikeService.removeById(id);
        return Result.ok();
    }

    /**
     * 批量上架商品（管理员）：走 insertBatchSomeColumn，一条 SQL 写多条。
     * 实验室页面的「批量插入对比」用的就是同一个方法。
     */
    @PostMapping("/batch")
    public Result<Map<String, Object>> batch(@RequestBody List<Bike> bikes, HttpSession session) {
        SessionUtil.requireAdmin(session);
        long start = System.currentTimeMillis();
        int rows = bikeService.importBatch(bikes);
        return Result.ok(Map.of(
                "rows", rows,
                "costMs", System.currentTimeMillis() - start,
                "tip", "insertBatchSomeColumn：一条 INSERT 多组 VALUES"));
    }
}
