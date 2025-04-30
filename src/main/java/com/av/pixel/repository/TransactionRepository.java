package com.av.pixel.repository;

import com.av.pixel.dao.Transactions;
import com.av.pixel.enums.OrderStatusEnum;
import com.av.pixel.enums.OrderTypeEnum;
import com.av.pixel.repository.base.BaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends BaseRepository<Transactions, String> {

    List<Transactions> findAllByUserCodeAndOrderIdAndDeletedFalse(String userCode, String orderId);

    Transactions findByOrderIdAndDeletedFalse(String orderId);

    Page<Transactions> findAllByDeletedFalseAndOrderTypeAndStatusIn(OrderTypeEnum orderType, List<OrderStatusEnum> orderStatusList, Pageable pageable);
}
