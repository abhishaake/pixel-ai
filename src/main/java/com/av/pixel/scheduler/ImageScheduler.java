package com.av.pixel.scheduler;

import com.av.pixel.dao.Generations;
import com.av.pixel.dao.PromptImage;
import com.av.pixel.enums.ImageCompressionConfig;
import com.av.pixel.helper.DateUtil;
import com.av.pixel.repository.GenerationsRepository;
import com.av.pixel.service.ImageCompressionService;
import com.av.pixel.service.S3Service;
import com.mongodb.client.result.UpdateResult;
import io.micrometer.common.util.StringUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.http.HttpResponse;
import java.util.List;
import java.util.Objects;

@Component
@Slf4j
@AllArgsConstructor
public class ImageScheduler {

    GenerationsRepository generationsRepository;
    MongoTemplate mongoTemplate;

    @Scheduled(cron = "0 0 1 * * ?")
    public void resetViews () {
        Long epoch2DaysAgo = DateUtil.currentTimeSec() - ( 2 * 24 * 60 * 60 );
        Long epoch1DayAgo = DateUtil.currentTimeSec() - ( 24 * 60 * 60 );
        String collectionName = "generations";

        Query query = new Query(
                Criteria.where("epoch").gt(epoch2DaysAgo).lte(epoch1DayAgo)
        );

        Update update = new Update().inc("views", -1000);

        UpdateResult result = mongoTemplate.updateMulti(
                query,
                update,
                collectionName
        );

        log.info("updated docs for view count: {} ", result.getModifiedCount());
    }


    S3Service s3Service;
    ImageCompressionService imageCompressionService;

    public void tempMethod () {
        List<Generations> generations = generationsRepository.findAll();

        for(Generations gen : generations) {
            for(PromptImage promptImage : gen.getImages()) {
                String url = promptImage.getUrl();
                String thumbnail = promptImage.getThumbnail();
                String fileName = url.replaceAll("https://av-pixel.s3.ap-south-1.amazonaws.com/","");
                fileName = fileName.replaceAll(".png","");
                if(StringUtils.isNotEmpty(thumbnail)) {
                   thumbnail = thumbnail.replaceAll("https://av-pixel.s3.ap-south-1.amazonaws.com/","");
                    s3Service.deleteFile(thumbnail);
                }
                HttpResponse<byte[]> imageRes = s3Service.downloadImage(url);

                double imageSize = imageCompressionService.getImageSize(imageRes.body());
                if (imageCompressionService.isCompressionRequired(imageSize)) {
                    ImageCompressionConfig config = imageCompressionService.getRequiredCompression(imageSize);
                    if (Objects.isNull(config)) {
                        thumbnail = url;
                    } else {
                        byte[] compressedImage = imageCompressionService.getCompressedImage(imageRes.body(), config);

                        thumbnail = s3Service.uploadToS3(compressedImage, fileName + "_thumbnail1"+ ".png");
                    }
                } else {
                    thumbnail = url;
                }
                promptImage.setThumbnail(thumbnail);
            }
            generationsRepository.save(gen);
            return;
        }
    }
}
