package com.av.pixel.response;

import com.av.pixel.dto.UserCreditDTO;
import com.av.pixel.dto.UserDTO;
import com.av.pixel.dto.UserTokenDTO;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class SignUpResponse {
    UserDTO user;
    UserCreditDTO userCredit;
    UserTokenDTO userToken;
}
