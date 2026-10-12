package org.example.mvc.model;

import lombok.Data;

/**
 * 上传文件时附带的 JSON 元数据（配合 @RequestPart 演示「文件 + JSON」混合提交）。
 */
@Data
public class FileMeta {

    private String title;
    private String remark;
}
