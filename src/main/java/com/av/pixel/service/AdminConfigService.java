package com.av.pixel.service;

public interface AdminConfigService {

    void loadAdminConfig ();

    Integer getDefaultCredits ();

    Integer getPrivacyUnlockCost ();

    boolean isIdeogramClientDisabled(String userCode);
}
