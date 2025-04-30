package com.av.pixel.repository;

import com.av.pixel.dao.UserCredit;
import com.av.pixel.repository.base.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserCreditRepository extends BaseRepository<UserCredit, String> {

    Optional<UserCredit> findByUserCodeAndDeletedFalse(String userCode);
}
