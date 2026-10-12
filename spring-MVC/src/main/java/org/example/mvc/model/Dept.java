package org.example.mvc.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 部门：用于演示自定义 Converter（String -> Dept）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Dept implements Serializable {

    private Long id;
    private String name;
}
