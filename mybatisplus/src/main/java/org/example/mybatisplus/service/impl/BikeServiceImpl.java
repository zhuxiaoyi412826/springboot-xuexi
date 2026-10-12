package org.example.mybatisplus.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import org.example.mybatisplus.common.BizException;
import org.example.mybatisplus.dto.BikeQuery;
import org.example.mybatisplus.dto.BikeVO;
import org.example.mybatisplus.dto.CategoryStatVO;
import org.example.mybatisplus.entity.Bike;
import org.example.mybatisplus.mapper.BikeMapper;
import org.example.mybatisplus.service.BikeService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BikeServiceImpl extends ServiceImpl<BikeMapper, Bike> implements BikeService {

    @Override
    public IPage<Bike> pageForSale(long current, long size, String keyword, String category) {
        return this.page(new Page<>(current, size), buildWrapper(keyword, category, true));
    }

    @Override
    public IPage<Bike> pageForAdmin(long current, long size, String keyword, String category) {
        return this.page(new Page<>(current, size), buildWrapper(keyword, category, false));
    }

    // ==================== 动态 SQL：XML / 注解 两套实现 ====================

    @Override
    public IPage<BikeVO> search(BikeQuery query, long current, long size, boolean useAnnotation) {
        Page<BikeVO> page = new Page<>(current, size);
        if (useAnnotation) {
            // 方式二：SQL 写在 BikeMapper 接口的 @Select("<script>...</script>") 里
            return baseMapper.searchWithAnnotation(page, query);
        }
        // 方式一：SQL 写在 resources/mapper/BikeMapper.xml
        return baseMapper.searchWithXml(page, query);
    }

    @Override
    public List<CategoryStatVO> stat(BikeQuery query, boolean useAnnotation) {
        if (useAnnotation) {
            return baseMapper.statWithAnnotation(query);
        }
        return baseMapper.statWithXml(query);
    }

    @Override
    public Map<String, List<String>> options() {
        List<Bike> all = this.list(Wrappers.<Bike>lambdaQuery()
                .select(Bike::getCategory, Bike::getBrand));
        List<String> categories = all.stream()
                .map(Bike::getCategory)
                .filter(StringUtils::hasText)
                .distinct()
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());
        List<String> brands = all.stream()
                .map(Bike::getBrand)
                .filter(StringUtils::hasText)
                .distinct()
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());
        Map<String, List<String>> map = new HashMap<>();
        map.put("categories", categories);
        map.put("brands", brands);
        return map;
    }

    private LambdaQueryWrapper<Bike> buildWrapper(String keyword, String category, boolean onlyOnSale) {
        LambdaQueryWrapper<Bike> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(Bike::getName, kw)
                    .or().like(Bike::getBrand, kw)
                    .or().like(Bike::getCategory, kw));
        }
        if (StringUtils.hasText(category)) {
            wrapper.eq(Bike::getCategory, category.trim());
        }
        if (onlyOnSale) {
            wrapper.eq(Bike::getStatus, 1);
        }
        wrapper.orderByDesc(Bike::getId);
        return wrapper;
    }

    @Override
    public boolean saveOrUpdate(Bike entity) {
        checkBike(entity);
        return super.saveOrUpdate(entity);
    }

    /**
     * 批量导入商品：走 insertBatchSomeColumn（一条 SQL 多组 VALUES）。
     * 每 500 条切一段，避免 SQL 过长超过 max_allowed_packet。
     */
    @Override
    public int importBatch(List<Bike> bikes) {
        if (bikes == null || bikes.isEmpty()) {
            throw new BizException("请至少选择一条商品");
        }
        List<Bike> valid = new ArrayList<>();
        for (Bike bike : bikes) {
            checkBike(bike);
            // 批量插入不会走逻辑删除/版本号的默认值注入，这里显式兜底
            if (bike.getDeleted() == null) {
                bike.setDeleted(0);
            }
            if (bike.getVersion() == null) {
                bike.setVersion(0);
            }
            valid.add(bike);
        }
        int rows = 0;
        for (int i = 0; i < valid.size(); i += 500) {
            List<Bike> chunk = valid.subList(i, Math.min(i + 500, valid.size()));
            rows += baseMapper.insertBatchSomeColumn(chunk);
        }
        return rows;
    }

    private void checkBike(Bike bike) {
        if (bike == null) {
            throw new BizException("商品信息不能为空");
        }
        if (!StringUtils.hasText(bike.getName())) {
            throw new BizException("商品名称不能为空");
        }
        if (bike.getPrice() == null || bike.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new BizException("价格不能为空且不能为负数");
        }
        if (bike.getStock() == null || bike.getStock() < 0) {
            throw new BizException("库存不能为空且不能为负数");
        }
        if (bike.getStatus() == null) {
            bike.setStatus(1);
        }
        if (bike.getId() == null) {
            Long count = this.lambdaQuery().eq(Bike::getName, bike.getName().trim()).count();
            if (count != null && count > 0) {
                throw new BizException("已存在同名商品");
            }
        }
    }
}
