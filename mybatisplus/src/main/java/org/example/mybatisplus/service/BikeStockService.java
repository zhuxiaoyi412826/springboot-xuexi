package org.example.mybatisplus.service;

import org.example.mybatisplus.common.BizException;
import org.example.mybatisplus.common.OptimisticConflictException;
import org.example.mybatisplus.entity.Bike;
import org.example.mybatisplus.mapper.BikeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 库存扣减（乐观锁版）。
 *
 * <p><b>为什么必须单独一个 Service、并且用 REQUIRES_NEW？</b>
 * MySQL 默认隔离级别是「可重复读」：同一个事务里不管查多少次，读到的都是事务开始时的快照。
 * 如果在下单的大事务里直接重试乐观锁，第二次 select 拿到的还是旧 version，
 * 于是 {@code UPDATE ... WHERE version = ?} 永远命中 0 行，重试多少次都白搭。
 * 把「读 + 乐观锁更新」放进一个独立事务，每次重试都是一个新事务、拿到新快照，重试才有意义。
 *
 * <p>代价：库存扣减会独立提交，下单主事务回滚时它不回滚（真要做电商需要补偿/消息事务），
 * 这里是为了把乐观锁讲清楚，注释留给你参考。
 */
@Service
public class BikeStockService {

    private final BikeMapper bikeMapper;

    public BikeStockService(BikeMapper bikeMapper) {
        this.bikeMapper = bikeMapper;
    }

    /**
     * 乐观锁扣库存：一次尝试（读 → 带 version 更新）。
     *
     * @throws OptimisticConflictException 版本冲突，调用方可以重试
     * @throws BizException                商品不存在 / 已下架 / 库存不足（这类重试也没用）
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public Bike deductOptimistic(Long bikeId, int quantity) {
        Bike bike = bikeMapper.selectById(bikeId);
        if (bike == null) {
            throw new BizException("自行车不存在");
        }
        if (bike.getStatus() == null || bike.getStatus() != 1) {
            throw new BizException("该商品已下架");
        }
        if (bike.getStock() == null || bike.getStock() < quantity) {
            throw new BizException("库存不足，当前库存：" + (bike.getStock() == null ? 0 : bike.getStock()));
        }
        bike.setStock(bike.getStock() - quantity);
        // OptimisticLockerInnerInterceptor 会把它变成：WHERE id = ? AND version = ?
        int rows = bikeMapper.updateById(bike);
        if (rows == 0) {
            throw new OptimisticConflictException("版本冲突：该行已被其它事务修改");
        }
        return bike;
    }
}
