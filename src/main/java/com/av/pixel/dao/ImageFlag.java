package com.av.pixel.dao;

import com.av.pixel.dao.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.data.mongodb.core.mapping.Document;

@EqualsAndHashCode(callSuper = true)
@Data
@Document(collection = "image_flags")
@Accessors(chain = true)
public class ImageFlag extends BaseEntity {

    private String genId;
    private String imageId;
    private String userCode;
    private String reason;
}
