package org.example.mybatisplus.enums;

/**
 * 下单时的并发控制方式（三种方案可以在「MP 实验室」页面里直接对比效果）。
 */
public enum LockMode {

    /** 原子 UPDATE：UPDATE ... SET stock = stock - n WHERE id = ? AND stock &gt;= n（无锁，推荐） */
    ATOMIC("atomic", "原子 UPDATE（无锁）"),

    /** 悲观锁：SELECT ... FOR UPDATE 锁住行，再在事务内扣减 */
    PESSIMISTIC("pessimistic", "悲观锁 FOR UPDATE"),

    /** 乐观锁：@Version 字段，UPDATE ... WHERE id = ? AND version = ?，冲突则重试/失败 */
    OPTIMISTIC("optimistic", "乐观锁 @Version");

    private final String code;
    private final String text;

    LockMode(String code, String text) {
        this.code = code;
        this.text = text;
    }

    public String getCode() {
        return code;
    }

    public String getText() {
        return text;
    }

    /** 按 code 解析，解析不了就用原子 UPDATE */
    public static LockMode of(String value) {
        if (value != null) {
            for (LockMode mode : values()) {
                if (mode.code.equalsIgnoreCase(value.trim()) || mode.name().equalsIgnoreCase(value.trim())) {
                    return mode;
                }
            }
        }
        return ATOMIC;
    }
}
