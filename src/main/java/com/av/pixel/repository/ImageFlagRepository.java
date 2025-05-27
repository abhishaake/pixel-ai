package com.av.pixel.repository;

import com.av.pixel.dao.ImageFlag;
import com.av.pixel.repository.base.BaseRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ImageFlagRepository  extends BaseRepository<ImageFlag, String> {
}
