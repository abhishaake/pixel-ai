package com.av.pixel.service;

import com.av.pixel.dao.Transactions;
import com.av.pixel.dao.User;
import com.av.pixel.dao.UserCredit;
import com.av.pixel.dto.UserCreditDTO;
import com.av.pixel.enums.OrderTypeEnum;

public interface UserCreditService {

    UserCredit createNewUserCredit(User user);

    UserCredit createNewUserCredit(String userCode);

    UserCreditDTO getUserCredit(User user);

    UserCreditDTO getUserCredit(String userCode);

    UserCreditDTO debitUserCredit (String userCode, Integer used, OrderTypeEnum orderType, String source, String orderId);

    UserCreditDTO creditUserCredits (String userCode, Integer credits);

    UserCreditDTO creditUserCredits (String userCode, Integer credits, Transactions transaction);
}
