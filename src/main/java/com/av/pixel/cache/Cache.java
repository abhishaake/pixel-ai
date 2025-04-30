package com.av.pixel.cache;

import com.av.pixel.dao.AdminConfig;
import com.av.pixel.dto.ModelPricingDTO;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.ConcurrentHashMap;

@Data
public class Cache {

    @Getter
    @Setter
    public static ConcurrentHashMap<String, ModelPricingDTO> modelPricingMap = new ConcurrentHashMap<>();

    @Getter
    @Setter
    public static ConcurrentHashMap<String, AdminConfig> adminConfigMap = new ConcurrentHashMap<>();

}
