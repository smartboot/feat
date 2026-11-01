package tech.smartboot.feat.demo.plus.common;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 统一把数据库时间格式化成前端展示用的文本。
 * <p>
 * 前端 mock 使用 Intl.DateTimeFormat("zh-CN") 产出 "yyyy-MM-dd HH:mm"，
 * 这里保持同样的格式，切换真实接口后页面上的时间显示才不会变。
 */
public final class DateFormats {
    private static final String PATTERN = "yyyy-MM-dd HH:mm";

    private DateFormats() {
    }

    public static String format(Date date) {
        return date == null ? null : new SimpleDateFormat(PATTERN).format(date);
    }

    public static String now() {
        return format(new Date());
    }
}
