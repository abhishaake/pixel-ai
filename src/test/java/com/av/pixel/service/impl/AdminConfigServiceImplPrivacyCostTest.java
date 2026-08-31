package com.av.pixel.service.impl;

import com.av.pixel.cache.Cache;
import com.av.pixel.dao.AdminConfig;
import com.av.pixel.repository.AdminConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AdminConfigServiceImplPrivacyCostTest {

    private static final String ADMIN_CONFIG_KEY = "ADMIN_CONFIG";

    @Mock
    private AdminConfigRepository adminConfigRepository;

    private AdminConfigServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AdminConfigServiceImpl(adminConfigRepository);
        Cache.setAdminConfigMap(new ConcurrentHashMap<>());
    }

    private void cacheConfig(AdminConfig config) {
        ConcurrentHashMap<String, AdminConfig> map = new ConcurrentHashMap<>();
        map.put(ADMIN_CONFIG_KEY, config);
        Cache.setAdminConfigMap(map);
    }

    @Test
    void returnsConfiguredCostWhenPresent() {
        cacheConfig(new AdminConfig().setPrivacyUnlockCost(75));

        assertThat(service.getPrivacyUnlockCost()).isEqualTo(75);
    }

    @Test
    void fallsBackToFiftyWhenFieldIsNull() {
        cacheConfig(new AdminConfig());

        assertThat(service.getPrivacyUnlockCost()).isEqualTo(50);
    }

    @Test
    void fallsBackToFiftyWhenConfigIsMissingEntirely() {
        assertThat(service.getPrivacyUnlockCost()).isEqualTo(50);
    }
}
