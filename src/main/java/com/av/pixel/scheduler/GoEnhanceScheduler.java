package com.av.pixel.scheduler;

import com.av.pixel.service.GenerationsService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@AllArgsConstructor
public class GoEnhanceScheduler {

    GenerationsService generationsService;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        generationsService.refreshVideoEffects();
    }

    @Scheduled(cron = "0 0 2 * * ?")
    public void refreshVideoEffects() {
        log.info("GoEnhanceScheduler: refreshing video effects");
        generationsService.refreshVideoEffects();
    }
}
