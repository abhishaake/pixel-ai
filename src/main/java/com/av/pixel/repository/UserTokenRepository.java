package com.av.pixel.repository;

import com.av.pixel.dao.User;
import com.av.pixel.dao.UserToken;
import com.av.pixel.repository.base.BaseRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserTokenRepository extends BaseRepository<UserToken, String> {
    UserToken findByAccessTokenAndExpiredFalseAndDeletedFalse(String accessToken);

    UserToken findByUserCodeAndExpiredFalseAndDeletedFalse(String userCode);
}
