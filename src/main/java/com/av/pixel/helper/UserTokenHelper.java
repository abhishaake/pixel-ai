package com.av.pixel.helper;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;

public class UserTokenHelper {

    private static final SecureRandom secureRandom = new SecureRandom();
    private static final Base64.Encoder base64Encoder = Base64.getUrlEncoder().withoutPadding();

    public static String generateToken() {
        byte[] randomBytes = new byte[16];
        secureRandom.nextBytes(randomBytes);
        return base64Encoder.encodeToString(randomBytes);
    }

    public static Long getDefaultValidity() {
        return DateUtil.getXYearAheadEpoch(1);
    }
}
