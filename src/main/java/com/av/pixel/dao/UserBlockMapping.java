package com.av.pixel.dao;

import com.av.pixel.dao.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.data.mongodb.core.mapping.Document;


@EqualsAndHashCode(callSuper = true)
@Data
@Document(collection = "user_block_mapping")
@Accessors(chain = true)
public class UserBlockMapping extends BaseEntity  {

    String userCode;

    String blockedUserCode;

    String blockedUserName;
}
