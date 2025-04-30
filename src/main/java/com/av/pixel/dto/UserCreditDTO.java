package com.av.pixel.dto;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class UserCreditDTO {
    String userCode;
    Integer available;
    Integer utilised;
}
