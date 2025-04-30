package com.av.pixel.helper;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;

public class DateUtil {

    public static Long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    public static Long currentTimeSec() {
        return System.currentTimeMillis() / 1000;
    }

    public static Long getXYearAheadEpoch(int years) {
        Instant now = Instant.now();
        ZonedDateTime oneYearLater = now.atZone(ZoneId.systemDefault()).plusYears(1);
        return oneYearLater.toInstant().toEpochMilli();
    }

    public static String formatDateTime(Date date) {
        SimpleDateFormat format = new SimpleDateFormat("MMM dd, yyyy HH:mm");
        return format.format(date);
    }

    public static String formatDate(Date date) {
        SimpleDateFormat format = new SimpleDateFormat("MMM dd, yyyy");
        return format.format(date);
    }

    public static String formatDate2(Date date) {
        SimpleDateFormat format = new SimpleDateFormat("MMMM, yyyy");
        return format.format(date);
    }

}
