package com.av.pixel.scheduler;

import com.av.pixel.service.GenerationsService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@AllArgsConstructor
public class VideoEffectJobScheduler {

    GenerationsService generationsService;

    @Scheduled(fixedDelay = 300000)
    public void processPendingVideoEffectJobs() {
        log.info("VideoEffectJobScheduler: checking pending jobs");
        generationsService.processPendingVideoEffectJobs();
    }
}
