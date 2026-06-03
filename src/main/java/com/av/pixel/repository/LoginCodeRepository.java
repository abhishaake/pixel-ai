package com.av.pixel.repository;

import com.av.pixel.dao.LoginCode;
import com.av.pixel.repository.base.BaseRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LoginCodeRepository extends BaseRepository<LoginCode, String> {

    LoginCode findByCodeAndDeletedFalse(String code);
}
