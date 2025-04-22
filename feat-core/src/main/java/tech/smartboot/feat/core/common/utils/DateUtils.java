/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.core.common.utils;

import org.smartboot.socket.timer.HashedWheelTimer;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * @author 三刀(zhengjunweimail @ 163.com)
 * @version v1.0.0
 */
public class DateUtils {
    private static final String COOKIE_PATTERN = "EEE, dd-MMM-yyyy HH:mm:ss z";
    public static final String RFC1123_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";
    private static final TimeZone GMT = TimeZone.getTimeZone("GMT");
    //必须采用该规范的字段：Date、Expires
    private static final ThreadLocal<SimpleDateFormat> RFC1123_DATE_FORMAT = ThreadLocal.withInitial(() -> {
        SimpleDateFormat sdf = new SimpleDateFormat(RFC1123_FORMAT, Locale.US);
        // rfc2616 3.3.1
        // All HTTP date/time stamps MUST be represented in Greenwich Mean Time (GMT), without exception.
        sdf.setTimeZone(GMT);
        return sdf;
    });

    private static final ThreadLocal<SimpleDateFormat> COOKIE_FORMAT = ThreadLocal.withInitial(() -> {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(COOKIE_PATTERN, Locale.US);
        simpleDateFormat.setTimeZone(GMT);
        return simpleDateFormat;
    });
    /**
     * 当前时间
     */
    private static final Date currentTime = new Date();
    private static byte[] rfc1123Bytes;

    static {
        rfc1123Bytes = DateUtils.formatRFC1123(currentTime).getBytes();
        HashedWheelTimer.DEFAULT_TIMER.scheduleWithFixedDelay(() -> {
            currentTime.setTime(System.currentTimeMillis());
            String date = DateUtils.formatRFC1123(currentTime);
            rfc1123Bytes = date.getBytes();
        }, 1, TimeUnit.SECONDS);
    }

    public static Date currentTime() {
        return currentTime;
    }

    public static Date parseRFC1123(String date) throws ParseException {
        return RFC1123_DATE_FORMAT.get().parse(date);
    }

    public static String formatRFC1123(Date date) {
        return RFC1123_DATE_FORMAT.get().format(date);
    }

    public static byte[] currentTimeFormatRFC1123() {
        return rfc1123Bytes;
    }

    public static String formatCookieExpire(Date date) {
        return COOKIE_FORMAT.get().format(date);
    }

}
