package com.av.pixel.repository;

import com.av.pixel.dao.UserBlockMapping;
import com.av.pixel.repository.base.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserBlockMappingRepository extends BaseRepository<UserBlockMapping, String> {

    List<UserBlockMapping> findByUserCodeAndDeletedFalse(String userCode);

    List<UserBlockMapping> findAllByUserCodeAndBlockedUserCodeAndDeletedFalse(String userCode, String blockedUser);
}
