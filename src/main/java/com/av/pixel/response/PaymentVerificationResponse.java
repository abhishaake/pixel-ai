package com.av.pixel.response;

import com.av.pixel.enums.PurchaseStatusEnum;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class PaymentVerificationResponse {
    PurchaseStatusEnum status;
    String orderId;
    String purchaseTime;
    String exceptionMsg;
    Integer userCredits;
    Boolean testUser;

    public PaymentVerificationResponse(PurchaseStatusEnum status,
                                       String orderId,
                                       String purchaseTime,
                                       String exceptionMsg,
                                       Integer userCredits) {
        this.status = status;
        this.orderId = orderId;
        this.purchaseTime = purchaseTime;
        this.exceptionMsg = exceptionMsg;
        this.userCredits = userCredits;
    }

    public PaymentVerificationResponse(PurchaseStatusEnum status, String orderId, String purchaseTime) {
        this.status = status;
        this.orderId = orderId;
        this.purchaseTime = purchaseTime;
    }

    @JsonIgnore
    public boolean isSuccess () {
        return PurchaseStatusEnum.SUCCESS.equals(status);
    }
}
