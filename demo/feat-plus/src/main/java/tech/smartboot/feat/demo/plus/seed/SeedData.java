package tech.smartboot.feat.demo.plus.seed;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * 演示数据的唯一来源。
 * <p>
 * 应用首次启动和调用 reset-demo 都从这里取数，避免种子数据散落在 SQL 与 Java 两处。
 * 所有账号的初始密码都是 admin123。
 */
public final class SeedData {

    public static final String PASSWORD_HASH =
            "120000$jX9qW0w9Lh8AESIzRFVmdw==$Ib5udaU8AP26Z4l1LXiBep1D0kIc0rWjea20RP7ftT8=";

    public static final String DEMO_USERNAME = "admin";

    private static final String TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    private SeedData() {
    }

    public static List<SeedUser> users() {
        return Arrays.asList(
                new SeedUser("admin", "系统管理员", "ADMIN", "平台研发部", "admin@feat.plus", 1, "2026-07-18 09:12:00", "2026-08-02 09:20:00"),
                new SeedUser("alice", "陈晓雨", "USER", "产品设计部", "alice@feat.plus", 1, "2026-07-22 11:30:00", "2026-08-01 16:15:00"),
                new SeedUser("bob", "周明远", "USER", "客户成功部", "bob@feat.plus", 1, "2026-07-23 14:02:00", "2026-07-30 10:42:00"),
                new SeedUser("carol", "林书雅", "AUDITOR", "财务审计部", "carol@feat.plus", 1, "2026-07-24 16:48:00", "2026-08-01 11:08:00"),
                new SeedUser("david", "郑可为", "USER", "平台研发部", "david@feat.plus", 0, "2026-07-25 10:20:00", "2026-08-02 08:32:00"),
                new SeedUser("ella", "苏婉宁", "USER", "市场运营部", "ella@feat.plus", 1, "2026-07-26 17:35:00", "2026-07-31 15:46:00"),
                new SeedUser("frank", "王逸飞", "USER", "客户成功部", "frank@feat.plus", 1, "2026-07-27 13:15:00", "2026-08-01 09:18:00"),
                new SeedUser("grace", "许清和", "AUDITOR", "财务审计部", "grace@feat.plus", 0, "2026-07-28 08:46:00", "2026-08-01 17:22:00"),
                new SeedUser("henry", "赵嘉树", "USER", "市场运营部", "henry@feat.plus", 1, "2026-07-29 15:12:00", "2026-08-02 08:55:00"),
                new SeedUser("iris", "唐语汐", "USER", "产品设计部", "iris@feat.plus", 1, "2026-07-30 12:06:00", "2026-08-01 14:03:00"),
                new SeedUser("jason", "蒋文博", "USER", "平台研发部", "jason@feat.plus", 1, "2026-08-01 09:40:00", "2026-08-02 09:06:00"),
                new SeedUser("kelly", "陆安然", "USER", "产品设计部", "kelly@feat.plus", 1, "2026-08-02 08:18:00", "2026-08-02 08:18:00")
        );
    }

    public static List<SeedActivity> activities() {
        List<SeedActivity> list = new ArrayList<>();
        list.add(new SeedActivity("enable", "管理员启用了用户 jason", "账号已恢复访问工作空间", "jason", "2026-08-02 09:06:00"));
        list.add(new SeedActivity("update", "陈晓雨更新了个人信息", "邮箱和所属部门已同步", "alice", "2026-08-02 08:48:00"));
        list.add(new SeedActivity("system", "审计员导出了登录日志", "导出范围为最近 30 天", "carol", "2026-08-02 08:02:00"));
        list.add(new SeedActivity("system", "系统完成每日数据备份", "演示环境状态正常", null, "2026-08-02 07:10:00"));
        return list;
    }

    public static List<SeedNotification> notifications() {
        List<SeedNotification> list = new ArrayList<>();
        list.add(new SeedNotification("2 个账号处于停用状态", "可前往用户管理查看并处理", 0, "2026-08-02 09:15:00"));
        list.add(new SeedNotification("每日备份已完成", "演示数据快照创建成功", 0, "2026-08-02 07:10:00"));
        list.add(new SeedNotification("欢迎使用 Feat Plus", "命令面板和用户管理功能现已可用", 1, "2026-08-01 18:00:00"));
        return Collections.unmodifiableList(list);
    }

    public static Date parseTime(String text) {
        try {
            return new SimpleDateFormat(TIME_PATTERN).parse(text);
        } catch (ParseException e) {
            throw new IllegalStateException("无法解析种子时间: " + text, e);
        }
    }

    public static final class SeedUser {
        public final String username;
        public final String displayName;
        public final String roleCode;
        public final String department;
        public final String email;
        public final int status;
        public final String createdTime;
        public final String editTime;

        SeedUser(String username, String displayName, String roleCode, String department, String email,
                 int status, String createdTime, String editTime) {
            this.username = username;
            this.displayName = displayName;
            this.roleCode = roleCode;
            this.department = department;
            this.email = email;
            this.status = status;
            this.createdTime = createdTime;
            this.editTime = editTime;
        }
    }

    public static final class SeedActivity {
        public final String kind;
        public final String title;
        public final String description;
        public final String username;
        public final String createdTime;

        SeedActivity(String kind, String title, String description, String username, String createdTime) {
            this.kind = kind;
            this.title = title;
            this.description = description;
            this.username = username;
            this.createdTime = createdTime;
        }
    }

    public static final class SeedNotification {
        public final String title;
        public final String description;
        public final int readFlag;
        public final String createdTime;

        SeedNotification(String title, String description, int readFlag, String createdTime) {
            this.title = title;
            this.description = description;
            this.readFlag = readFlag;
            this.createdTime = createdTime;
        }
    }
}
