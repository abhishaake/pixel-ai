package com.av.pixel.dao;

import com.av.pixel.dao.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.data.mongodb.core.mapping.Document;

@EqualsAndHashCode(callSuper = true)
@Data
@Document(collection = "user_credit")
@Accessors(chain = true)
public class UserCredit extends BaseEntity {

    String userCode;

    Integer available;

    Integer utilised;
}
