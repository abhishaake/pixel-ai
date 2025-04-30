package com.av.pixel.helper;

import com.av.pixel.dao.GenerationHistory;
import com.av.pixel.dao.Generations;
import com.av.pixel.dto.GenerationsDTO;
import com.av.pixel.enums.ImageStyleEnum;
import com.av.pixel.mapper.GenerationsMap;
import com.av.pixel.repository.GenerationHistoryRepository;
import com.av.pixel.repository.GenerationsRepository;
import com.av.pixel.request.GenerateRequest;
import com.av.pixel.request.ideogram.ImageRequest;
import com.av.pixel.response.ideogram.ImageResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;

@Component
@Slf4j
@AllArgsConstructor
public class GenerationHelper {

    GenerationsRepository generationsRepository;

    GenerationHistoryRepository generationHistoryRepository;

    MongoTemplate mongoTemplate;

    private static final List<String> ITEMS = Arrays.asList(
            "https://av-pixel.s3.ap-south-1.amazonaws.com/a-3d-cartoon-scene-featuring-a-yellow-fi_byJ3Pz7pSOycGstNgRUvNg_LiI8hrqzTZ-QXXac5zcU1w.jpeg",
            "https://av-pixel.s3.ap-south-1.amazonaws.com/a-captivating-cinematic-painting-depicti_C1CYut-vRma2gyfer6_Nlw_aBFx5xAuQH-deyqz42OkoA.jpeg",
            "https://av-pixel.s3.ap-south-1.amazonaws.com/a-captivating-gorgeous-woman-walks-grace_pnK-CTVaRcCFU_msaKJLoA__VKjmrAPQTSjMpOGTgw2zw.jpeg",
            "https://av-pixel.s3.ap-south-1.amazonaws.com/a-captivating-illustration-of-a-cat-scur_Cpf87RoeRH6vat42o-BJ2A_YPvt4-G0SjimeaMARsv4Dw.jpeg",
            "https://av-pixel.s3.ap-south-1.amazonaws.com/a-conceptual-art-piece-depicting-nibiru-__1AQiGh2QmKfTyrSMMAkYQ_2X3QjRH-QA6BLwrilLYHDw.jpeg",
            "https://av-pixel.s3.ap-south-1.amazonaws.com/a-digital-illustration-depicting-a-fract_Tz4vdH4pRnqL97A_gcaaLQ_JgF5W05iTZWTPTbBHbHfpg.jpeg",
            "https://av-pixel.s3.ap-south-1.amazonaws.com/a-mesmerizing-surreal-dreamscape-by-miki_6OijJvZgSV2KfbD7ZcVAtQ_0Y0UPyI8Qo-IaONEsGuESg.jpeg",
            "https://av-pixel.s3.ap-south-1.amazonaws.com/a-modern-oil-painting-and-alcoholic-ink-_W9hl1U-nT-uDy2_xhkkycw_ckk_fNrwT2GZsnjR6v-qig.jpeg",
            "https://av-pixel.s3.ap-south-1.amazonaws.com/a-pencil-sketch-of-a-serene-moment-betwe_1IXsXdsQTIaoA0-_RauJRg_w1PwQaxvS6CuhQbaR7GdYA.jpeg",
            "https://av-pixel.s3.ap-south-1.amazonaws.com/a-striking-anthropomorphic-poppy-flower-_HtuDKe7fTUuP29nWAPs_PA_6UFajTNRQtet3oXmobsJhw.jpeg",
            "https://av-pixel.s3.ap-south-1.amazonaws.com/a-stunning-ultra-high-definition-close-u_vb6_FB6ESpuGzYEXSgZ4ig_3GTqynvVRG63FKBtpB8DEQ.jpeg",
            "https://av-pixel.s3.ap-south-1.amazonaws.com/a-surreal-abstract-conceptual-photo-of-a_6byk86A5RfGue1cCuz5BZA_LlVBNNWpRuKNKzV4XxFWlg.jpeg",
            "https://av-pixel.s3.ap-south-1.amazonaws.com/an-exquisite-surreal-scene-of-an-azure-p_lSyZBQgWQRyOSOyqoF4K0Q_IROVHRYlQ1yuLjjVetXeOw.jpeg",
            "https://av-pixel.s3.ap-south-1.amazonaws.com/teen-angst-ethereal-incandescence-anthro_1V46LyMfRbGNtu4Xf9WJUw_Iv_UWFp6TAOJG6dnigqhPg.jpeg",
            "https://av-pixel.s3.ap-south-1.amazonaws.com/teen-angst-ethereal-incandescence-anthro_4wLJP6wITkWFGUoF27MUmg_VHE2JCEbTI2T6MaEwk5_kA.jpeg",
            "https://av-pixel.s3.ap-south-1.amazonaws.com/teen-angst-ethereal-incandescence-anthro_lXGRZtzHTsKo1N-75diUTg_SLld4dldQEKsaaRoQY0ILA.jpeg",
            "https://av-pixel.s3.ap-south-1.amazonaws.com/teen-angst-ethereal-incandescence-anthro_VvxVX2EfQtmbTJW-cssnLA_gDwTxFMhQiynZyjYBioE8Q.jpeg"
    );

    private static final Random RANDOM = new Random();

    public List<ImageResponse> generateImages(ImageRequest imageRequest) {
        try{
            Thread.sleep(5000);
        }
        catch (Exception e){
            log.error(e.getMessage(), e);
        }
        List<ImageResponse> res = new ArrayList<>();

        for(int i=0;i<imageRequest.getNumberOfImages();i++) {
            ImageResponse imageResponse = new ImageResponse();
            imageResponse.setPrompt("test magic prompt " + i +1);
            imageResponse.setResolution("1024x1024");
            imageResponse.setIsImageSafe(true);
            imageResponse.setSeed(Objects.isNull(imageRequest.getSeed()) ? 1234 : imageRequest.getSeed());
            imageResponse.setUrl(ITEMS.get(RANDOM.nextInt(ITEMS.size())));
            imageResponse.setStyleType(Objects.isNull(imageRequest.getStyleType()) ? null : imageRequest.getStyleType().name());
            res.add(imageResponse);
        }

        return res;
    }

    public Generations saveUserGeneration (String userCode, GenerateRequest generateRequest, ImageRequest imageRequest, List<ImageResponse> imageResponses, Integer imageGenerationCost) {
        Generations generations = GenerationsMap.toGenerationsEntity(userCode, generateRequest.getModel(), generateRequest.getPrompt(),
                generateRequest.getRenderOption(), generateRequest.getPrivateImage(), Objects.nonNull(imageRequest.getStyleType()) ? imageRequest.getStyleType().name() : ImageStyleEnum.AUTO.name(),
                generateRequest.getColorPalette(), imageRequest.getAspectRatio(), imageResponses);

        if (Objects.isNull(imageRequest.getStyleType())) {
            generations.setStyle(ImageStyleEnum.AUTO.name());
        }

        generations = generationsRepository.save(generations);
        String id = generations.getId().toString();

        GenerationHistory generationHistory = new GenerationHistory()
                .setGenerationId(id)
                .setUserCode(generations.getUserCode())
                .setCost(Double.valueOf(imageGenerationCost));

        generationHistoryRepository.save(generationHistory);

        return generations;
    }

    public Generations getById (String id) {
        try {
            ObjectId objectId = new ObjectId(id);
            Query query = new Query(Criteria.where("_id").is(objectId));
            return mongoTemplate.findOne(query, Generations.class);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
