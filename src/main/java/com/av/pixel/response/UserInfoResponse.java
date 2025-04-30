package com.av.pixel.response;

import com.av.pixel.dto.UserCreditDTO;
import com.av.pixel.dto.UserDTO;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class UserInfoResponse {
    UserDTO user;
    UserCreditDTO userCredit;
}
