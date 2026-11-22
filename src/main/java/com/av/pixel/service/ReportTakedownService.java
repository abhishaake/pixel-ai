package com.av.pixel.service;

import com.av.pixel.request.ImageReportRequest;

/**
 * Records user reports, and withdraws a generation from the public feed once enough
 * distinct users have reported it.
 */
public interface ReportTakedownService {

    void report(String userCode, ImageReportRequest request);
}
