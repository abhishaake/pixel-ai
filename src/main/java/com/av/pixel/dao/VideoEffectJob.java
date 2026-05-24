package com.av.pixel.dao;

import com.av.pixel.dao.base.BaseEntity;
import com.av.pixel.enums.VideoEffectJobStatusEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.data.mongodb.core.mapping.Document;

@EqualsAndHashCode(callSuper = true)
@Data
@Document(collection = "video_effect_jobs")
@Accessors(chain = true)
public class VideoEffectJob extends BaseEntity {

    String imgUuid;
    String userCode;
    String effect;
    String resolution;
    String referenceImageUrl;
    Boolean privateImage;
    VideoEffectJobStatusEnum status;
}
