package com.av.pixel.repository;

import com.av.pixel.dao.ModelPricing;
import com.av.pixel.repository.base.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ModelPricingRepository extends BaseRepository<ModelPricing, String> {

    List<ModelPricing> findAllByDeletedFalse();
}
