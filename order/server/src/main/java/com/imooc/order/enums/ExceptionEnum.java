package com.imooc.order.enums;

import lombok.Getter;

/**
 * Created by 廖师兄
 * 2017-12-10 17:32
 */
@Getter
public enum ExceptionEnum {
    PARAM_ERROR(1, "参数错误"),
    CART_EMPTY(2, "购物车为空"),
    ORDER_NOT_EXIST(3, "订单不存在"),
    ORDER_STATUS_ERROR(4, "订单状态错误"),
    ORDER_DETAIL_NOT_EXIST(5, "订单详情不存在"),
    SECKILL_QUANTITY_ERROR(6, "一次只能秒杀一个商品"),
    DECREASE_STOCK_ERROR(7, "扣库存失败"),
    SECKILL_ORDER_ERROR(8, "秒杀下单异常")
    ;

    private Integer code;

    private String message;

    ExceptionEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
