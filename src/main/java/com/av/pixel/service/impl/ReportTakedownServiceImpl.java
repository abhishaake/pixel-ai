package com.av.pixel.service.impl;

import com.av.pixel.dao.Generations;
import com.av.pixel.dao.ImageFlag;
import com.av.pixel.repository.ImageFlagRepository;
import com.av.pixel.request.ImageReportRequest;
import com.av.pixel.service.ReportTakedownService;
import com.av.pixel.service.SesEmailService;
import com.mongodb.client.result.UpdateResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Slf4j
@Service
public class ReportTakedownServiceImpl implements ReportTakedownService {

    private final ImageFlagRepository imageFlagRepository;
    private final MongoTemplate mongoTemplate;
    private final SesEmailService sesEmailService;
    private final int hideThreshold;

    public ReportTakedownServiceImpl(ImageFlagRepository imageFlagRepository,
                                     MongoTemplate mongoTemplate,
                                     SesEmailService sesEmailService,
                                     @Value("${moderation.report-hide-threshold:3}") int hideThreshold) {
        this.imageFlagRepository = imageFlagRepository;
        this.mongoTemplate = mongoTemplate;
        this.sesEmailService = sesEmailService;
        this.hideThreshold = hideThreshold;
    }

    @Override
    public void report(String userCode, ImageReportRequest request) {
        if (Objects.isNull(request) || StringUtils.isEmpty(request.getGenId())) {
            return;
        }
        String genId = request.getGenId();

        // One report per user per generation; repeats must not move the count.
        if (StringUtils.isNotEmpty(userCode) && imageFlagRepository.existsByGenIdAndUserCode(genId, userCode)) {
            return;
        }
        imageFlagRepository.save(new ImageFlag()
                .setGenId(genId)
                .setImageId(request.getImageId())
                .setReason(request.getReason())
                .setUserCode(userCode));

        // Distinct, identified reporters only: anonymous reports and duplicate rows
        // must not let one person hide someone else's post.
        long reporters = imageFlagRepository.findByGenId(genId).stream()
                .map(ImageFlag::getUserCode)
                .filter(StringUtils::isNotEmpty)
                .distinct()
                .count();
        if (reporters >= hideThreshold) {
            hideFromFeed(genId, reporters);
        }
    }

    private void hideFromFeed(String genId, long reporters) {
        try {
            Query query = new Query(Criteria.where("_id").is(new ObjectId(genId))
                    .and("reportHidden").ne(true));
            UpdateResult result = mongoTemplate.updateFirst(
                    query, new Update().set("privateImage", true).set("reportHidden", true),
                    Generations.class);
            if (result.getModifiedCount() > 0) {
                log.warn("generation hidden from feed after {} distinct reports, genId={}", reporters, genId);
                alert(genId, reporters);
            }
        } catch (Exception e) {
            log.error("[CRITICAL] could not hide reported generation genId={}", genId, e);
        }
    }

    private void alert(String genId, long reporters) {
        try {
            sesEmailService.sendErrorMail("[MODERATION] generation hidden from public feed"
                    + "\n\n genId : " + genId
                    + "\n\n distinct reporters : " + reporters
                    + "\n\n It has been set private. Review the reports in image_flags.");
        } catch (Exception e) {
            log.error("could not send report takedown alert", e);
        }
    }
}
