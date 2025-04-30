package com.av.pixel.dao;


import com.av.pixel.dao.base.BaseEntity;
import com.av.pixel.enums.OrderStatusEnum;
import com.av.pixel.enums.OrderTypeEnum;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.data.mongodb.core.mapping.Document;

@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@Document("transactions")
public class Transactions extends BaseEntity {
    String userCode;

    Integer creditsBefore;
    Integer creditsInvolved;
    Integer creditsAfter;

    OrderTypeEnum orderType;
    String orderId;
    String packageId;
    OrderStatusEnum status;
    String amountInRs;

    String source;
    String remarks;

    Integer retries;

    @JsonIgnore
    public Integer getValidRetries() {
        return this.retries != null ? this.retries : 0;
    }
}
