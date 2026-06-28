package com.av.pixel.dao;

import com.av.pixel.dao.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@EqualsAndHashCode(callSuper = true)
@Data
@Document(collection = "video_effect_configs")
@Accessors(chain = true)
public class VideoEffectConfig extends BaseEntity {

    @Indexed(unique = true)
    String effectId;
    String label;
    String url;
    boolean intimate;
}
