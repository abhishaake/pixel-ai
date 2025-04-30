package com.av.pixel.service;

public interface AdminConfigService {

    void loadAdminConfig ();

    Integer getDefaultCredits ();

    boolean isIdeogramClientDisabled(String userCode);
}
