package com.av.pixel.repository;

import com.av.pixel.dao.ImageFlag;
import com.av.pixel.repository.base.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImageFlagRepository  extends BaseRepository<ImageFlag, String> {

    boolean existsByGenIdAndUserCode(String genId, String userCode);

    List<ImageFlag> findByGenId(String genId);
}
