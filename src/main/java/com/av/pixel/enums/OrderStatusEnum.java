package com.av.pixel.enums;

import java.util.List;

public enum OrderStatusEnum {
    INITIATED,
    PENDING,
    SUCCESS,
    FAILED,
    ERROR,
    RETRY;


    public static List<OrderStatusEnum> getFailedStatusList () {
        return List.of(INITIATED, PENDING, FAILED, ERROR, RETRY);
    }
}
