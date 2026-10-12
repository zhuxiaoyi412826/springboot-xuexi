package org.example.mybatis.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 小说分类
 *
 * <p>注意：本类实现了 {@link Serializable} —— 这是使用 MyBatis 二级缓存的前提，
 * 二级缓存会把对象序列化后存入缓存介质。
 */
@Data
public class Category implements Serializable {

    private Long id;
    private String name;
    private String description;
    private Integer sortNum;
    private LocalDateTime createTime;
    private Integer deleted;

    /** 一对多：该分类下的小说（CategoryMapper.xml 的 {@code <collection>} 映射） */
    private List<Novel> novels;
}
