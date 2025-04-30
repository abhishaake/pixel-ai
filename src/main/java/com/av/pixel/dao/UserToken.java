package com.av.pixel.dao;

import com.av.pixel.dao.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.data.mongodb.core.mapping.Document;

@EqualsAndHashCode(callSuper = true)
@Data
@Document(collection = "user_token")
@Accessors(chain = true)
public class UserToken extends BaseEntity {

    String userCode;

    String accessToken;

    String authToken;

    Long validity;

    boolean expired;
}
