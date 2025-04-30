package com.av.pixel.scheduler;

import com.av.pixel.cache.Cache;
import com.av.pixel.dto.ModelPricingDTO;
import com.av.pixel.service.AdminConfigService;
import com.av.pixel.service.ModelPricingService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
@AllArgsConstructor
public class CacheScheduler {

    ModelPricingService modelPricingService;
    AdminConfigService adminConfigService;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        loadModelPricing();
        loadAdminConfig();
    }

    @Scheduled(cron = "0 0 * * * ?")
    public void loadModelPricing() {
        log.info("loadModelPricing scheduler started");
        List<ModelPricingDTO> modelPricingDTOS = modelPricingService.getAllModelPricingList();

        if (CollectionUtils.isEmpty(modelPricingDTOS)) {
            return;
        }
        ConcurrentHashMap<String, ModelPricingDTO> modelPricingMap = new ConcurrentHashMap<>();

        for(ModelPricingDTO modelPricingDTO : modelPricingDTOS) {
            String model = modelPricingDTO.getModel();
            modelPricingMap.put(model, modelPricingDTO);
        }

        Cache.setModelPricingMap(modelPricingMap);
    }

    @Scheduled(cron = "0 0 * * * ?")
    public void loadAdminConfig() {
        log.info("loadAdminConfig scheduler started");
        adminConfigService.loadAdminConfig();
    }
}
