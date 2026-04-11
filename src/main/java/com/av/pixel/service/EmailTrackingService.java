package com.av.pixel.service;

import jakarta.servlet.http.HttpServletRequest;

import java.net.URI;

public interface EmailTrackingService {

    URI trackClickAndResolveRedirect(String uid, String cid, String redirect, String platform,
                                     HttpServletRequest request);

    void trackOpen(String uid, String cid, HttpServletRequest request);

    byte[] transparentTrackingGif();
}
