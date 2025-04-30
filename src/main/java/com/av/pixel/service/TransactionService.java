package com.av.pixel.service;

import com.av.pixel.dao.Transactions;
import com.av.pixel.enums.OrderStatusEnum;
import com.av.pixel.enums.OrderTypeEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TransactionService {

    Transactions saveTransaction(String userCode, Integer creditsBefore, Integer creditsInvolved, Integer creditsAfter,
                                 OrderTypeEnum orderType, String orderId, String packageId, OrderStatusEnum status,
                                 String amountInRs, String source, String remarks);

    Transactions saveTransaction(Transactions transaction);

    Transactions createOrUpdateTransaction (String userCode, Integer creditsBefore, Integer creditsInvolved, Integer creditsAfter,
                                            OrderTypeEnum orderType, String orderId, String packageId, OrderStatusEnum status,
                                            String amountInRs, String source, String remarks);

    Transactions getTransactionByOrderId (String orderId);

    Page<Transactions> getFailedTransactions (OrderTypeEnum orderType, Pageable pageable);
}
