package com.av.pixel.service;

import jakarta.servlet.http.HttpServletRequest;

import java.net.URI;

public interface EmailTrackingService {

    URI trackClickAndResolveRedirect(String uid, String cid, String redirect, String platform,
                                     String emailId, HttpServletRequest request);

    void trackOpen(String uid, String cid, String emailId, HttpServletRequest request);

    byte[] transparentTrackingGif();
}
