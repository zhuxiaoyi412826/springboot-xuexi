package org.example.redis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/** 歌曲。 */
@Data
@TableName("song")
public class Song implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String singer;
    private String album;
    private String category;
    /** 时长（秒） */
    private Integer duration;
    private String coverUrl;
    private String audioUrl;
    private String lyric;
    /** 1 上架 0 下架 */
    private Integer status;
    private Long playCount;
    private Long likeCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
