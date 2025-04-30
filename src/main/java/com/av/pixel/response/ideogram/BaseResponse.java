package com.av.pixel.response.ideogram;

import lombok.Data;

import java.util.List;

@Data
public class BaseResponse<T> {
    String created;
    List<T> data;
}
