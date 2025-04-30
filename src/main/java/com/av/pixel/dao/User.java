package com.av.pixel.dao;

import com.av.pixel.dao.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.data.mongodb.core.mapping.Document;

@EqualsAndHashCode(callSuper = true)
@Data
@Document(collection = "users")
@Accessors(chain = true)
public class User extends BaseEntity {

    String firstName;

    String lastName;

    String email;

    String phone;

    String code;

    String password;

    String imageUrl;

}
