package org.example.mybatis;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 小说阅读器启动类
 *
 * <p>{@code @MapperScan} 扫描 Mapper 接口；另一种做法是在每个 Mapper 上加 {@code @Mapper}，二选一即可。
 */
@SpringBootApplication
@MapperScan("org.example.mybatis.mapper")
public class MybatisApplication {

    public static void main(String[] args) {
        SpringApplication.run(MybatisApplication.class, args);
    }

}
