package org.example.mybatis.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 收藏
 */
@Data
public class Favorite implements Serializable {

    private Long id;
    private Long userId;
    private Long novelId;
    private LocalDateTime createTime;
}
