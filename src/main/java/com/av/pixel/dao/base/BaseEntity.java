package com.av.pixel.dao.base;

import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;

import java.util.Date;

@Data
public abstract class BaseEntity {

    @Id
    private ObjectId id;

    @CreatedDate
    private Date created;

    @LastModifiedDate
    private Date updated;

    private boolean deleted;
}
