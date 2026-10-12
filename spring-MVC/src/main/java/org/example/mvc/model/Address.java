package org.example.mvc.model;

import lombok.Data;

import java.io.Serializable;

/**
 * 嵌套绑定演示：address.province / address.city / address.detail
 */
@Data
public class Address implements Serializable {

    private String province;
    private String city;
    private String detail;
}
