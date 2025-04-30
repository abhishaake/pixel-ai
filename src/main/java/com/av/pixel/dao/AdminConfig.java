package com.av.pixel.dao;

import com.av.pixel.dao.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Document(collection = "admin_config")
@Data
@Accessors(chain = true)
public class AdminConfig extends BaseEntity {

    private String key;
    private Integer defaultNewUserCredit;
    private Boolean isIdeogramDown;
    private Boolean enableTestingEnv;


    private List<String> userCodesForTesting;
    private Boolean isTestingEnabledForUsers;
}
