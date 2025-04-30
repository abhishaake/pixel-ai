package com.av.pixel.helper;

import com.av.pixel.dao.UserCredit;
import com.av.pixel.service.AdminConfigService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@AllArgsConstructor
public class UserCreditHelper {

    private final AdminConfigService adminConfigService;

    public Integer getDefaultUserCredit() {
        return adminConfigService.getDefaultCredits();
    }
}
