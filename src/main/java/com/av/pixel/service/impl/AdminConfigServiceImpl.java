package com.av.pixel.service.impl;

import com.av.pixel.cache.Cache;
import com.av.pixel.dao.AdminConfig;
import com.av.pixel.exception.Error;
import com.av.pixel.repository.AdminConfigRepository;
import com.av.pixel.service.AdminConfigService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@AllArgsConstructor
public class AdminConfigServiceImpl implements AdminConfigService {

    private final AdminConfigRepository adminConfigRepository;
    private static final String ADMIN_CONFIG_KEY = "ADMIN_CONFIG";

    @Override
    public void loadAdminConfig () {
        AdminConfig adminConfig = adminConfigRepository.findByKeyAndDeletedFalse(ADMIN_CONFIG_KEY);

        if (Objects.isNull(adminConfig)) {
            throw new Error("Admin config not found");
        }

        ConcurrentHashMap<String, AdminConfig> adminConfigMap = new ConcurrentHashMap<>();
        adminConfigMap.put(ADMIN_CONFIG_KEY, adminConfig);

        Cache.setAdminConfigMap(adminConfigMap);
    }

    @Override
    public Integer getDefaultCredits () {
        AdminConfig adminConfig = Cache.adminConfigMap.get(ADMIN_CONFIG_KEY);

        if (Objects.isNull(adminConfig) || Objects.isNull(adminConfig.getDefaultNewUserCredit())) {
            return 0;
        }
        return adminConfig.getDefaultNewUserCredit();
    }

    @Override
    public boolean isIdeogramClientDisabled (String userCode) {
        AdminConfig adminConfig = Cache.adminConfigMap.get(ADMIN_CONFIG_KEY);

        if (Objects.isNull(adminConfig) || Objects.isNull(adminConfig.getEnableTestingEnv())) {
            return true;
        }

        if (!adminConfig.getEnableTestingEnv()) {
            if (Boolean.TRUE.equals(adminConfig.getIsTestingEnabledForUsers()) && !CollectionUtils.isEmpty(adminConfig.getUserCodesForTesting())
                && adminConfig.getUserCodesForTesting().contains(userCode)) {
                return true;
            }
        }
        return adminConfig.getEnableTestingEnv();
    }


}
