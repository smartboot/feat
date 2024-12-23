/*******************************************************************************
 * Copyright (c) 2024, tech.smartboot. All rights reserved.
 * project name: feat
 * file name: DateUtils.java
 * Date: 2020-12-03
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package tech.smartboot.feat.core.common.utils;

import org.smartboot.socket.timer.HashedWheelTimer;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * @author 三刀
 * @version V1.0 , 2020/12/3
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
    private static long currentTimeMillis = System.currentTimeMillis();

    static {
        HashedWheelTimer.DEFAULT_TIMER.scheduleWithFixedDelay(() -> currentTimeMillis = System.currentTimeMillis(), 1, TimeUnit.SECONDS);
    }

    public static long currentTimeMillis() {
        return currentTimeMillis;
    }

    public static Date parseRFC1123(String date) throws ParseException {
        return RFC1123_DATE_FORMAT.get().parse(date);
    }

    public static String formatRFC1123(Date date) {
        return RFC1123_DATE_FORMAT.get().format(date);
    }

    public static String formatCookieExpire(Date date) {
        return COOKIE_FORMAT.get().format(date);
    }

}
