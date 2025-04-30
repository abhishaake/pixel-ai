package com.av.pixel.dao;

import com.av.pixel.dao.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@Document(collection = "generations")
@Accessors(chain = true)
public class Generations extends BaseEntity {

    String userCode;
    List<PromptImage> images;
    String tag;
    String category;
    String model;
    String userPrompt;
    Long likes;
    String renderOption;
    Long seed;
    String resolution;
    Boolean privateImage;
    String style;
    String colorPalette;
    String aspectRatio;
}
