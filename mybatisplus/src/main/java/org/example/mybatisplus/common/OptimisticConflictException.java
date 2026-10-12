package org.example.mybatisplus.common;

/**
 * 乐观锁版本冲突：UPDATE ... WHERE version = ? 影响行数为 0。
 *
 * <p>单独定义成一种异常，方便上层区分「库存真的不够」和「被并发抢了，可以重试」。
 */
public class OptimisticConflictException extends RuntimeException {

    public OptimisticConflictException(String message) {
        super(message);
    }
}
