package org.example.mybatisplus.dto;

import lombok.Data;

/**
 * 下单请求参数
 */
@Data
public class OrderRequest {

    /** 自行车 id */
    private Long bikeId;

    /** 购买数量，默认 1 */
    private Integer quantity;

    /** 收货人 */
    private String receiver;

    /** 联系电话 */
    private String phone;

    /** 收货地址 */
    private String address;
}
