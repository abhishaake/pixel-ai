package com.av.pixel.service.impl;

import com.av.pixel.cache.RLock;
import com.av.pixel.dao.Packages;
import com.av.pixel.dao.Transactions;
import com.av.pixel.dto.UserCreditDTO;
import com.av.pixel.enums.OrderStatusEnum;
import com.av.pixel.enums.OrderTypeEnum;
import com.av.pixel.enums.PurchaseStatusEnum;
import com.av.pixel.exception.Error;
import com.av.pixel.google.PurchaseProcessingService;
import com.av.pixel.repository.PackageRepository;
import com.av.pixel.request.PaymentVerificationRequest;
import com.av.pixel.response.PaymentVerificationResponse;
import com.av.pixel.service.MonetizationService;
import com.av.pixel.service.NotificationService;
import com.av.pixel.service.TransactionService;
import com.av.pixel.service.UserCreditService;
import io.micrometer.common.util.StringUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.weaver.ast.Or;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@Slf4j
@AllArgsConstructor
public class MonetizationServiceImpl implements MonetizationService {

    private final PackageRepository packageRepository;
    private final UserCreditService userCreditService;
    private final TransactionService transactionService;
    private final PurchaseProcessingService purchaseProcessingService;
    private final RLock rLock;
    private final NotificationService notificationService;

    @Override
    public void handleAdPayment (String userCode, String adIdentifier, String adTxnId, String timestamp) {
        if (StringUtils.isEmpty(userCode)) {
            throw new Error("user code empty");
        }
        Transactions transaction = createTransactionEntity(userCode, OrderTypeEnum.AD_CREDIT, adTxnId, adIdentifier, OrderStatusEnum.INITIATED, "AD");

        if (StringUtils.isEmpty(adIdentifier)) {
            saveErrorTransaction(transaction, "empty ad identifier");
            return;
        }

        Packages packageInfo = packageRepository.getByPackageIdAndDeletedFalse(adIdentifier);

        if (Objects.isNull(packageInfo)) {
            saveErrorTransaction(transaction, "package not found");
            return;
        }

        UserCreditDTO userCreditDTO = userCreditService.creditUserCredits(userCode, packageInfo.getCredits(), transaction);

        notificationService.addNewTokenNotification(userCode, packageInfo.getCredits());
    }


    @Override
    public PaymentVerificationResponse handleGooglePayment (PaymentVerificationRequest paymentVerificationRequest) {
        String userCode = paymentVerificationRequest.getUserCode();
        String productId = paymentVerificationRequest.getProductId();
        String purchaseToken = paymentVerificationRequest.getPurchaseToken();

        if (StringUtils.isEmpty(userCode)) {
            throw new Error("user code empty");
        }
        if (StringUtils.isEmpty(purchaseToken)) {
            throw new Error("purchaseToken empty");
        }
        String lockKey = "payment_" + purchaseToken.trim();
        boolean locked = rLock.tryLock(lockKey, 500);
        if (!locked) {
            log.error("{} cannot lock", lockKey);
            return new PaymentVerificationResponse(
                    PurchaseStatusEnum.ERROR,
                    null,
                    null,
                    "Locked",
                    null
            );
        }

        try {
            Transactions transaction = transactionService.getTransactionByOrderId(purchaseToken);
            if (Objects.nonNull(transaction) && OrderStatusEnum.SUCCESS.equals(transaction.getStatus())) {
                throw new Error("duplicate order");
            }

            if (Objects.isNull(transaction)) {
                transaction = createTransactionEntity(userCode, OrderTypeEnum.PURCHASE_CREDIT, purchaseToken, productId, OrderStatusEnum.INITIATED, "GOOGLE_PAYMENTS");
            } else {
                transaction.setStatus(OrderStatusEnum.RETRY);
                transaction.setRetries(transaction.getValidRetries() + 1);
                transaction = transactionService.saveTransaction(transaction);
            }

            try{
                if (StringUtils.isEmpty(productId)) {
                    saveErrorTransaction(transaction, "product id not found");
                    return new PaymentVerificationResponse();
                }
                Packages packageInfo = packageRepository.getByPackageIdAndDeletedFalse(productId);

                if (Objects.isNull(packageInfo)) {
                    saveErrorTransaction(transaction, "package not found");
                    return new PaymentVerificationResponse();
                }

                PaymentVerificationResponse paymentVerificationResponse = purchaseProcessingService.processPurchase(productId, purchaseToken);
                if (paymentVerificationResponse.isSuccess()) {
                    UserCreditDTO userCreditDTO = userCreditService.creditUserCredits(userCode, packageInfo.getCredits(), transaction);
                    paymentVerificationResponse.setUserCredits(userCreditDTO.getAvailable());
                    notificationService.sendPaymentSuccessNotification(userCode, packageInfo.getCredits());
                }
                else handleTransaction(transaction, paymentVerificationResponse);

                return paymentVerificationResponse;
            }
            catch (Exception e) {
                log.error("[CRITICAL] error : {}", e.getMessage(), e);
                saveErrorTransaction(transaction, "Exception occurred : " + e.getMessage());
                return new PaymentVerificationResponse(
                        PurchaseStatusEnum.ERROR,
                        null,
                        null,
                        e.getMessage(),
                        null
                );
            }
        } finally {
            rLock.unlock(lockKey);
        }
    }

    private void handleTransaction (Transactions transaction, PaymentVerificationResponse paymentVerificationResponse) {
        switch (paymentVerificationResponse.getStatus()) {
            case ERROR -> saveErrorTransaction(transaction, paymentVerificationResponse.getExceptionMsg());
            case PENDING -> savePendingTransaction(transaction, "in pending state");
            case FAILED -> saveFailedTransaction(transaction, "Payment failed or cancelled");
            default -> saveErrorTransaction(transaction, "unknown error");
        }
    }

    private Transactions createTransactionEntity (String userCode, OrderTypeEnum orderType, String orderId, String packageId, OrderStatusEnum orderStatus, String source) {
        return transactionService.saveTransaction(userCode, null, null, null, orderType,
                orderId, packageId, orderStatus, null, source, null);
    }

    private void saveErrorTransaction (Transactions transaction, String remarks) {
        transaction.setStatus(OrderStatusEnum.ERROR)
                .setRemarks(remarks);
        transactionService.saveTransaction(transaction);
    }

    private void saveFailedTransaction (Transactions transaction, String remarks) {
        transaction.setStatus(OrderStatusEnum.FAILED)
                .setRemarks(remarks);
        transactionService.saveTransaction(transaction);
    }

    private void savePendingTransaction (Transactions transaction, String remarks) {
        transaction.setStatus(OrderStatusEnum.PENDING)
                .setRemarks(remarks);
        transactionService.saveTransaction(transaction);
    }
}
