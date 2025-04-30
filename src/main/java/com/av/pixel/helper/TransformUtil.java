package com.av.pixel.helper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TransformUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public static <T> T fromJson(String jsonString, Class<T> valueType) {
        try {
            if (jsonString != null) {
                return objectMapper.readValue(jsonString, valueType);
            }
        } catch (Exception e) {
            log.error(
                    "Error in fromJson(), jsonString: " + jsonString + " ; Exception: " + e.getMessage(), e);
        }
        return null;
    }
}
