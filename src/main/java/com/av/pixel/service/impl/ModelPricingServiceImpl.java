package com.av.pixel.service.impl;

import com.av.pixel.dao.ModelPricing;
import com.av.pixel.dto.ModelPricingDTO;
import com.av.pixel.mapper.ModelPricingMap;
import com.av.pixel.repository.ModelPricingRepository;
import com.av.pixel.service.ModelPricingService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@AllArgsConstructor
public class ModelPricingServiceImpl implements ModelPricingService {

    private final ModelPricingRepository modelPricingRepository;

    @Override
    public List<ModelPricingDTO> getAllModelPricingList () {

        List<ModelPricing> modelPricingList = modelPricingRepository.findAllByDeletedFalse();

        return ModelPricingMap.toDtoList(modelPricingList);
    }
}
