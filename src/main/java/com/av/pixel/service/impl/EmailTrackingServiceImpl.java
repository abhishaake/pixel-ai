package com.av.pixel.service.impl;

import com.av.pixel.dao.EmailTrackingEvent;
import com.av.pixel.enums.EmailTrackingEventType;
import com.av.pixel.exception.Error;
import com.av.pixel.repository.EmailTrackingEventRepository;
import com.av.pixel.service.EmailTrackingService;
import io.micrometer.common.util.StringUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailTrackingServiceImpl implements EmailTrackingService {

    private final EmailTrackingEventRepository emailTrackingEventRepository;

    private static final byte[] TRANSPARENT_GIF_1X1 = new byte[]{
            0x47, 0x49, 0x46, 0x38, 0x39, 0x61, 0x01, 0x00,
            0x01, 0x00, (byte) 0x80, 0x01, 0x00, 0x00, 0x00, 0x00,
            (byte) 0xff, (byte) 0xff, (byte) 0xff, 0x21, (byte) 0xf9, 0x04,
            0x01, 0x00, 0x00, 0x00, 0x00, 0x2c, 0x00, 0x00, 0x00, 0x00,
            0x01, 0x00, 0x01, 0x00, 0x00, 0x02, 0x01, 0x44, 0x00, 0x3b
    };

    @Override
    public URI trackClickAndResolveRedirect(String uid, String cid, String redirect, String platform,
                                            String emailId, HttpServletRequest request) {
        validatePlatform(platform);
        URI target = validateAndParseRedirect(redirect);
        String ua = emptyIfNull(request.getHeader("User-Agent"));
        String ip = clientIp(request);
        String platformLog = normalizePlatformForLog(platform);
        String emailIdNorm = normalizeEmailId(emailId);
        Instant ts = Instant.now();
        log.info("emailClick userId={} campaignId={} emailId={} platform={} timestamp={} userAgent={} clientIp={}",
                uid, cid, emptyIfNull(emailIdNorm), platformLog, ts, ua, ip);
        persist(new EmailTrackingEvent()
                .setEventType(EmailTrackingEventType.CLICK)
                .setUserId(uid)
                .setCampaignId(cid)
                .setEmailId(emailIdNorm)
                .setPlatform(platformLog.isEmpty() ? null : platformLog)
                .setRedirectUrl(target.toString())
                .setUserAgent(ua)
                .setClientIp(ip)
                .setEventTimestamp(ts));
        return target;
    }

    @Override
    public void trackOpen(String uid, String cid, String emailId, HttpServletRequest request) {
        String ua = emptyIfNull(request.getHeader("User-Agent"));
        String ip = clientIp(request);
        String emailIdNorm = normalizeEmailId(emailId);
        Instant ts = Instant.now();
        log.info("emailOpen userId={} campaignId={} emailId={} timestamp={} userAgent={} clientIp={}",
                uid, cid, emptyIfNull(emailIdNorm), ts, ua, ip);
        persist(new EmailTrackingEvent()
                .setEventType(EmailTrackingEventType.OPEN)
                .setUserId(uid)
                .setCampaignId(cid)
                .setEmailId(emailIdNorm)
                .setUserAgent(ua)
                .setClientIp(ip)
                .setEventTimestamp(ts));
    }

    @Override
    public byte[] transparentTrackingGif() {
        return TRANSPARENT_GIF_1X1;
    }

    private static void validatePlatform(String platform) {
        if (StringUtils.isEmpty(platform)) {
            return;
        }
        String p = platform.trim().toLowerCase();
        if (!"ios".equals(p) && !"android".equals(p)) {
            throw new Error(HttpStatus.BAD_REQUEST, "platform must be ios or android when provided");
        }
    }

    private static String normalizePlatformForLog(String platform) {
        return StringUtils.isEmpty(platform) ? "" : platform.trim();
    }

    private URI validateAndParseRedirect(String redirect) {
        if (StringUtils.isEmpty(redirect)) {
            throw new Error(HttpStatus.BAD_REQUEST, "redirect is required");
        }
        String decoded = URLDecoder.decode(redirect, StandardCharsets.UTF_8);
        URI uri;
        try {
            uri = new URI(decoded);
        } catch (URISyntaxException e) {
            throw new Error(HttpStatus.BAD_REQUEST, "Invalid redirect URL");
        }
        String scheme = uri.getScheme();
        if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
            throw new Error(HttpStatus.BAD_REQUEST, "redirect must use http or https");
        }
        if (uri.getHost() == null || uri.getHost().isEmpty()) {
            throw new Error(HttpStatus.BAD_REQUEST, "Invalid redirect URL host");
        }
        return uri;
    }

    private static String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (StringUtils.isNotEmpty(xff)) {
            int comma = xff.indexOf(',');
            return (comma < 0 ? xff : xff.substring(0, comma)).trim();
        }
        String xri = request.getHeader("X-Real-IP");
        if (StringUtils.isNotEmpty(xri)) {
            return xri.trim();
        }
        return request.getRemoteAddr();
    }

    private static String emptyIfNull(String s) {
        return s == null ? "" : s;
    }

    private static String normalizeEmailId(String emailId) {
        if (emailId == null) {
            return null;
        }
        String trimmed = emailId.trim();
        return StringUtils.isEmpty(trimmed) ? null : trimmed;
    }

    private void persist(EmailTrackingEvent event) {
        try {
            String emailId = event.getEmailId();
            if (StringUtils.isNotEmpty(emailId)
                    && emailTrackingEventRepository.existsByEmailIdAndEventTypeAndPlatform(emailId, event.getEventType(), event.getPlatform())) {
                return;
            }
            emailTrackingEventRepository.save(event);
        } catch (DuplicateKeyException e) {
            log.debug("Duplicate email tracking skipped emailId={} type={}", event.getEmailId(), event.getEventType());
        } catch (Exception e) {
            log.error("Failed to persist email tracking event type={} userId={} campaignId={}",
                    event.getEventType(), event.getUserId(), event.getCampaignId(), e);
        }
    }
}
