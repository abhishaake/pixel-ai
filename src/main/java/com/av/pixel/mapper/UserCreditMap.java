package com.av.pixel.mapper;

import com.av.pixel.dao.UserCredit;
import com.av.pixel.dto.UserCreditDTO;

import java.util.Objects;

public class UserCreditMap {

    public static UserCreditDTO userCreditDTO (UserCredit userCredit) {
        if (Objects.isNull(userCredit)) {
            return null;
        }
        return new UserCreditDTO()
                .setUserCode(userCredit.getUserCode())
                .setAvailable(userCredit.getAvailable())
                .setUtilised(userCredit.getUtilised());
    }
}
