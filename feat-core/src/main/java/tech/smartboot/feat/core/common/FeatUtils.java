/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.core.common;

import org.smartboot.socket.timer.HashedWheelTimer;
import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;
import tech.smartboot.feat.core.common.utils.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * @author 三刀(zhengjunweimail @ 163.com)
 * @version v1.0.0
 */
public class FeatUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(FeatUtils.class);
    public static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
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

    static {
        HashedWheelTimer.DEFAULT_TIMER.scheduleWithFixedDelay(() -> {
            currentTime.setTime(System.currentTimeMillis());
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


    public static String formatCookieExpire(Date date) {
        return COOKIE_FORMAT.get().format(date);
    }

    public static String getResourceAsString(String fileName) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (InputStream inputStream = StringUtils.class.getClassLoader().getResourceAsStream(fileName);) {
            if (inputStream == null) {
                LOGGER.error("resource {} not found", fileName);
                return null;
            }
            byte[] buffer = new byte[1024];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
            }
            return outputStream.toString();
        } catch (IOException e) {
            LOGGER.error("read resource {} error", fileName, e);
            return null;
        }
    }

    public static byte[] toByteArray(InputStream inputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, len);
        }
        return outputStream.toByteArray();
    }

    public static String asString(InputStream inputStream) throws IOException {
        return new String(toByteArray(inputStream));
    }


    public static int toInt(final String str, final int defaultValue) {
        if (str == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(str);
        } catch (final NumberFormatException nfe) {
            return defaultValue;
        }
    }

    public static long toLong(final String str, final long defaultValue) {
        if (str == null) {
            return defaultValue;
        }
        try {
            return Long.parseLong(str);
        } catch (final NumberFormatException nfe) {
            return defaultValue;
        }
    }

    public static <T> boolean isEmpty(Collection<T> collection) {
        return collection == null || collection.isEmpty();
    }

    public static <T> boolean isNotEmpty(Collection<T> collection) {
        return !isEmpty(collection);
    }

    public static final String GZIP_ENCODE_UTF_8 = "UTF-8";
    public static final String GZIP_ENCODE_ISO_8859_1 = "ISO-8859-1";

    public static byte[] gzip(byte[] data, int offset, int length) {
        return gzip(data, offset, length, GZIP_ENCODE_UTF_8);
    }

    public static byte[] gzip(byte[] data, int offset, int length, String encoding) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        GZIPOutputStream gzip;
        try {
            gzip = new GZIPOutputStream(out);
            gzip.write(data, offset, length);
            gzip.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return out.toByteArray();
    }

    public static byte[] ungzip(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        try {
            GZIPInputStream ungzip = new GZIPInputStream(in);
            byte[] buffer = new byte[256];
            int n;
            while ((n = ungzip.read(buffer)) >= 0) {
                out.write(buffer, 0, n);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return out.toByteArray();
    }

    public static String ungzipToString(byte[] bytes, String encoding) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        try {
            GZIPInputStream ungzip = new GZIPInputStream(in);
            byte[] buffer = new byte[256];
            int n;
            while ((n = ungzip.read(buffer)) >= 0) {
                out.write(buffer, 0, n);
            }
            return out.toString(encoding);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String ungzipToString(byte[] bytes) {
        return ungzipToString(bytes, GZIP_ENCODE_UTF_8);
    }

    private static final String DOMAIN = "$Domain";
    private static final String VERSION = "$Version";
    private static final String PATH = "$Path";

    /**
     * 解码URI中的参数
     *
     * @param paramStr http参数字符串： aa=111&bb=222
     * @param paramMap 参数存放Map
     */
    public static void decodeParamString(String paramStr, Map<String, String[]> paramMap) {
        if (StringUtils.isBlank(paramStr)) {
            return;
        }
        String[] uriParamStrArray = StringUtils.split(paramStr, "&");
        for (String param : uriParamStrArray) {
            int index = param.indexOf("=");
            if (index == -1) {
                continue;
            }
            try {
                String key = URLDecoder.decode(StringUtils.substring(param, 0, index), "utf8");
                String value = URLDecoder.decode(StringUtils.substring(param, index + 1), "utf8");
                String[] values = paramMap.get(key);
                if (values == null) {
                    paramMap.put(key, new String[]{value});
                } else {
                    String[] newValue = new String[values.length + 1];
                    System.arraycopy(values, 0, newValue, 0, values.length);
                    newValue[values.length] = value;
                    paramMap.put(key, newValue);
                }

            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
    }

    public static List<Cookie> decodeCookies(String cookieStr) {
        List<Cookie> cookies = new ArrayList<>();
        decode(cookies, cookieStr, 0, new HashMap<>());
        return cookies;
    }

    private static void decode(List<Cookie> cookies, String cookieStr, int offset, Map<String, String> cache) {
        while (offset < cookieStr.length() && cookieStr.charAt(offset) == ' ') {
            offset++;
        }
        if (offset >= cookieStr.length()) {
            return;
        }
        int index = cookieStr.indexOf('=', offset);
        if (index == -1) {
            return;
        }
        String name = cookieStr.substring(offset, index);
        int end = cookieStr.indexOf(';', index);
        int trimEnd = end;
        if (trimEnd == -1) {
            trimEnd = cookieStr.length();
        }
        while (cookieStr.charAt(trimEnd - 1) == ' ') {
            trimEnd--;
        }
        String value = cookieStr.substring(index + 1, trimEnd);

        if (name.charAt(0) == '$') {
            if (cookies.isEmpty()) {
                cache.put(name, value);
            } else {
                Cookie cookie = cookies.get(cookies.size() - 1);
                switch (name) {
                    case DOMAIN:
                        cookie.setDomain(value);
                        break;
                    case PATH:
                        cookie.setPath(value);
                        break;
                }
            }
        } else {
            Cookie cookie = new Cookie(name, value);
            if (!cache.isEmpty()) {
                cache.forEach((key, v) -> {
                    switch (key) {
                        case DOMAIN:
                            cookie.setDomain(v);
                            break;
                        case PATH:
                            cookie.setPath(v);
                            break;
                    }
                });
                cache.clear();
            }
            cookies.add(cookie);
        }
        if (end != -1) {
            decode(cookies, cookieStr, end + 1, cache);
        }
    }


}
