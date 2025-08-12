package com.av.pixel.service.impl;

import com.av.pixel.dao.Transactions;
import com.av.pixel.dao.User;
import com.av.pixel.dao.UserCredit;
import com.av.pixel.dto.UserCreditDTO;
import com.av.pixel.enums.OrderStatusEnum;
import com.av.pixel.enums.OrderTypeEnum;
import com.av.pixel.helper.UserCreditHelper;
import com.av.pixel.mapper.UserCreditMap;
import com.av.pixel.repository.UserCreditRepository;
import com.av.pixel.service.TransactionService;
import com.av.pixel.service.UserCreditService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@Slf4j
@AllArgsConstructor
public class UserCreditServiceImpl implements UserCreditService {

    private final UserCreditRepository userCreditRepository;
    private final UserCreditHelper userCreditHelper;
    private final TransactionService transactionService;
    private final EmailService emailService;

    @Override
    public UserCredit createNewUserCredit (User user) {

        UserCredit userCredit = userCreditRepository.findByUserCodeAndDeletedFalse(user.getCode()).orElse(null);

        if (Objects.nonNull(userCredit)) {
            return userCredit;
        }

        userCredit = new UserCredit()
                .setAvailable(userCreditHelper.getDefaultUserCredit())
                .setUserCode(user.getCode())
                .setUtilised(0);

        return userCreditRepository.save(userCredit);
    }

    @Override
    public UserCredit createNewUserCredit (String userCode) {

        UserCredit userCredit = userCreditRepository.findByUserCodeAndDeletedFalse(userCode).orElse(null);

        if (Objects.nonNull(userCredit)) {
            return userCredit;
        }

        userCredit = new UserCredit()
                .setAvailable(userCreditHelper.getDefaultUserCredit())
                .setUserCode(userCode)
                .setUtilised(0);

        return userCreditRepository.save(userCredit);
    }

    @Override
    public UserCreditDTO getUserCredit (User user) {
        UserCredit userCredit = userCreditRepository.findByUserCodeAndDeletedFalse(user.getCode()).orElse(null);

        return UserCreditMap.userCreditDTO(userCredit);
    }

    @Override
    public UserCreditDTO getUserCredit (String userCode) {
        UserCredit userCredit = userCreditRepository.findByUserCodeAndDeletedFalse(userCode).orElse(null);

        return UserCreditMap.userCreditDTO(userCredit);
    }

    @Override
    public UserCreditDTO debitUserCredit (String userCode, Integer used, OrderTypeEnum orderType, String source, String orderId) {
        UserCredit userCredit = userCreditRepository.findByUserCodeAndDeletedFalse(userCode).orElse(null);

        assert userCredit != null;
        Integer available = userCredit.getAvailable();
        userCredit.setAvailable(available - used);

        Integer utilised = userCredit.getUtilised();
        userCredit.setUtilised(utilised + used);

        UserCreditDTO userCreditDTO = UserCreditMap.userCreditDTO(userCreditRepository.save(userCredit));

        transactionService.saveTransaction(userCode, available, used, userCredit.getAvailable(), orderType,
                orderId, null, OrderStatusEnum.SUCCESS, null, source, null);

        emailService.sendMilestoneMail(userCode);

        return userCreditDTO;
    }

    @Override
    public UserCreditDTO creditUserCredits (String userCode, Integer credits) {
        UserCredit userCredit = userCreditRepository.findByUserCodeAndDeletedFalse(userCode).orElse(null);

        assert userCredit != null;
        Integer available = userCredit.getAvailable();
        userCredit.setAvailable(available + credits);

        return UserCreditMap.userCreditDTO(userCreditRepository.save(userCredit));
    }

    @Override
    public UserCreditDTO creditUserCredits (String userCode, Integer credits, Transactions transaction) {
        UserCredit userCredit = userCreditRepository.findByUserCodeAndDeletedFalse(userCode).orElse(null);

        assert userCredit != null;
        Integer available = userCredit.getAvailable();
        userCredit.setAvailable(available + credits);

        transaction.setCreditsBefore(available)
                .setCreditsAfter(userCredit.getAvailable())
                .setCreditsInvolved(credits)
                .setStatus(OrderStatusEnum.SUCCESS);

        transactionService.saveTransaction(transaction);

        return UserCreditMap.userCreditDTO(userCreditRepository.save(userCredit));
    }
}
