package org.example.mybatisplus.mapper;

import org.example.mybatisplus.entity.TenantGoods;

/**
 * 多租户演示 Mapper（无需写 tenant_id 条件，插件自动加）
 */
public interface TenantGoodsMapper extends EasyBaseMapper<TenantGoods> {
}
