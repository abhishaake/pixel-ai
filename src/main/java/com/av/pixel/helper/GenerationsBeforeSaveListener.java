package com.av.pixel.helper;

import com.av.pixel.dao.Generations;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.BeforeSaveEvent;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class GenerationsBeforeSaveListener extends AbstractMongoEventListener<Generations> {

    @Override
    public void onBeforeSave(BeforeSaveEvent<Generations> event) {
        Generations document = event.getSource();

        if (document.getLikes() == null) {
            document.setLikes(0L);
        }
        if (document.getCreated() == null) {
            document.setCreated(new Date());
        }
        document.setUpdated(new Date());
        super.onBeforeSave(event);
    }
}
