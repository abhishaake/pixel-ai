package com.av.pixel.scheduler;

import com.av.pixel.dao.Transactions;
import com.av.pixel.enums.OrderTypeEnum;
import com.av.pixel.request.PaymentVerificationRequest;
import com.av.pixel.service.MonetizationService;
import com.av.pixel.service.TransactionService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;

@Component
@Slf4j
@AllArgsConstructor
public class PaymentScheduler {

    TransactionService transactionService;
    MonetizationService monetizationService;

    public void handleFailedPayments () {

        Page<Transactions> failedTxns = transactionService.getFailedTransactions(OrderTypeEnum.PURCHASE_CREDIT, PageRequest.of(0,20));

        if (Objects.isNull(failedTxns) || CollectionUtils.isEmpty(failedTxns.getContent())) {
            return;
        }
        List<Transactions> txns = failedTxns.getContent();

        for (Transactions txn : txns) {
            String userCode = txn.getUserCode();
            String orderId = txn.getOrderId();
            String productId = txn.getPackageId();

            log.info("handleFailedPayments for {}, {}, {}", userCode, productId, orderId);

            PaymentVerificationRequest req = new PaymentVerificationRequest(userCode, productId, orderId);
            log.info("res for userCode: {} : {}", userCode, monetizationService.handleGooglePayment(req));
        }

    }
}
