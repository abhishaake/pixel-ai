package com.av.pixel.dao;

import com.av.pixel.dao.base.BaseEntity;
import com.av.pixel.enums.ModerationDecisionEnum;
import com.av.pixel.enums.ModerationSourceEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

/**
 * One refused upload. Only rejections are stored, which keeps write volume near zero
 * while still producing the evidence the Google Play appeal needs.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Document(collection = "upload_moderation_logs")
@Accessors(chain = true)
public class UploadModerationLog extends BaseEntity {

    private String userCode;
    private ModerationSourceEnum source;
    private ModerationDecisionEnum decision;
    private String topLabel;
    private Double topConfidence;
    private List<String> labels;
    private String contentType;
    private long sizeBytes;
}
