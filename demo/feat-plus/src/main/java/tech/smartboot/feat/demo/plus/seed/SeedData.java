package tech.smartboot.feat.demo.plus.seed;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import tech.smartboot.feat.demo.plus.common.DataScope;
import tech.smartboot.feat.demo.plus.common.Permission;

/**
 * 演示数据的唯一来源。
 * <p>
 * 应用首次启动和调用 reset-demo 都从这里取数，避免种子数据散落在 SQL 与 Java 两处。
 * 所有账号的初始密码都是 admin123。
 * <p>
 * 写入顺序由引用关系决定：角色 → 部门（先父后子）→ 用户 → 用户角色关联。
 * <p>
 * alice 刻意挂了两个角色（USER + DEPT_MANAGER），用来同时演示权限并集与数据范围取最宽：
 * 她只能看到「产品设计部」的人，而不是全公司。
 */
public final class SeedData {

    public static final String PASSWORD_HASH =
            "120000$jX9qW0w9Lh8AESIzRFVmdw==$Ib5udaU8AP26Z4l1LXiBep1D0kIc0rWjea20RP7ftT8=";

    public static final String DEMO_USERNAME = "admin";

    private static final String TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    private SeedData() {
    }

    /**
     * 三个内置角色。ADMIN 拥有全部权限且不可停用、不可改权限，是系统的兜底通道；
     * AUDITOR 只读；USER 仅能看数据概览。自定义角色可以在角色管理页新增。
     */
    public static List<SeedRole> roles() {
        return Arrays.asList(
                new SeedRole("ADMIN", "系统管理员", "拥有全部权限，内置角色不可删除、不可停用",
                        Permission.join(Permission.ALL), DataScope.ALL, 1, 1),
                new SeedRole("DEPT_MANAGER", "部门主管", "只能看到本部门及以下成员的账号，用于演示数据权限",
                        Permission.join(Arrays.asList(Permission.DASHBOARD_READ, Permission.USER_READ)),
                        DataScope.DEPARTMENT_AND_CHILD, 1, 1),
                new SeedRole("AUDITOR", "审计员", "只读查看成员、角色、部门与审计日志，不能做任何变更",
                        Permission.join(Arrays.asList(Permission.DASHBOARD_READ, Permission.USER_READ,
                                Permission.ROLE_READ, Permission.DEPT_READ, Permission.AUDIT_READ)),
                        DataScope.ALL, 1, 1),
                new SeedRole("USER", "普通成员", "登录后可访问数据概览，看不到管理菜单",
                        Permission.join(Collections.singletonList(Permission.DASHBOARD_READ)),
                        DataScope.SELF, 1, 1)
        );
    }

    /**
     * 部门树，按先父后子的顺序列出。parentCode 为空表示根部门。
     */
    public static List<SeedDepartment> departments() {
        return Arrays.asList(
                new SeedDepartment("TECH", "", "技术中心", 1, "研发、测试与基础设施"),
                new SeedDepartment("TECH_RD", "TECH", "平台研发部", 1, "后端服务与平台能力"),
                new SeedDepartment("TECH_RD_FE", "TECH_RD", "前端组", 1, "管理后台与前端组件"),
                new SeedDepartment("TECH_RD_BE", "TECH_RD", "后端组", 2, "接口与数据链路"),
                new SeedDepartment("TECH_QA", "TECH", "质量保障部", 2, "测试与发布质量"),
                new SeedDepartment("PMC", "", "产品与市场中心", 2, "产品设计与市场运营"),
                new SeedDepartment("PMC_DESIGN", "PMC", "产品设计部", 1, "产品规划与交互设计"),
                new SeedDepartment("PMC_MARKET", "PMC", "市场运营部", 2, "品牌与增长"),
                new SeedDepartment("CS", "", "客户成功部", 3, "客户交付与支持"),
                new SeedDepartment("FIN", "", "财务审计部", 4, "财务与内控审计")
        );
    }

    public static List<SeedUser> users() {
        return Arrays.asList(
                new SeedUser("admin", "系统管理员", Arrays.asList("ADMIN"), "TECH_RD", "admin@feat.plus", 1, "2026-07-18 09:12:00", "2026-08-02 09:20:00"),
                new SeedUser("alice", "陈晓雨", Arrays.asList("USER", "DEPT_MANAGER"), "PMC_DESIGN", "alice@feat.plus", 1, "2026-07-22 11:30:00", "2026-08-01 16:15:00"),
                new SeedUser("bob", "周明远", Arrays.asList("USER"), "CS", "bob@feat.plus", 1, "2026-07-23 14:02:00", "2026-07-30 10:42:00"),
                new SeedUser("carol", "林书雅", Arrays.asList("AUDITOR"), "FIN", "carol@feat.plus", 1, "2026-07-24 16:48:00", "2026-08-01 11:08:00"),
                new SeedUser("david", "郑可为", Arrays.asList("USER"), "TECH_RD_FE", "david@feat.plus", 0, "2026-07-25 10:20:00", "2026-08-02 08:32:00"),
                new SeedUser("ella", "苏婉宁", Arrays.asList("USER"), "PMC_MARKET", "ella@feat.plus", 1, "2026-07-26 17:35:00", "2026-07-31 15:46:00"),
                new SeedUser("frank", "王逸飞", Arrays.asList("USER"), "CS", "frank@feat.plus", 1, "2026-07-27 13:15:00", "2026-08-01 09:18:00"),
                new SeedUser("grace", "许清和", Arrays.asList("AUDITOR"), "FIN", "grace@feat.plus", 0, "2026-07-28 08:46:00", "2026-08-01 17:22:00"),
                new SeedUser("henry", "赵嘉树", Arrays.asList("USER"), "PMC_MARKET", "henry@feat.plus", 1, "2026-07-29 15:12:00", "2026-08-02 08:55:00"),
                new SeedUser("iris", "唐语汐", Arrays.asList("USER"), "PMC_DESIGN", "iris@feat.plus", 1, "2026-07-30 12:06:00", "2026-08-01 14:03:00"),
                new SeedUser("jason", "蒋文博", Arrays.asList("USER"), "TECH_RD_FE", "jason@feat.plus", 1, "2026-08-01 09:40:00", "2026-08-02 09:06:00"),
                new SeedUser("kelly", "陆安然", Arrays.asList("USER"), "TECH_RD_BE", "kelly@feat.plus", 1, "2026-08-02 08:18:00", "2026-08-02 08:18:00")
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

    /**
     * 站内通知。第三个参数是接收人：0 表示广播（所有人都能收到），大于 0 表示只投递给这个人。
     * <p>
     * 这里刻意把两类混着放，用来演示「广播 + 定向」并存时，每个人的列表与未读数都各自独立。
     * 已读状态不在这张表里，而是每人一行记在 sys_notification_read。
     */
    public static List<SeedNotification> notifications() {
        List<SeedNotification> list = new ArrayList<>();
        list.add(new SeedNotification("2 个账号处于停用状态", "可前往用户管理查看并处理", 0, "2026-08-02 09:15:00"));
        list.add(new SeedNotification("每日备份已完成", "演示数据快照创建成功", 0, "2026-08-02 07:10:00"));
        list.add(new SeedNotification("欢迎使用 Feat Plus", "命令面板和用户管理功能现已可用", 1, "2026-08-01 18:00:00"));
        list.add(new SeedNotification("你的资料待完善", "请补充手机号，便于接收安全提醒", 2, "2026-08-01 16:20:00"));
        list.add(new SeedNotification("本季度审计任务已分配", "请在审计日志中核对 7 月的账号变更", 4, "2026-08-01 11:05:00"));
        return Collections.unmodifiableList(list);
    }

    public static Date parseTime(String text) {
        try {
            return new SimpleDateFormat(TIME_PATTERN).parse(text);
        } catch (ParseException e) {
            throw new IllegalStateException("无法解析种子时间: " + text, e);
        }
    }

    public static final class SeedRole {
        public final String code;
        public final String name;
        public final String description;
        public final String permissions;
        /** 数据范围档位，取值见 DataScope。 */
        public final int departmentScope;
        public final int builtin;
        public final int status;

        SeedRole(String code, String name, String description, String permissions, int departmentScope,
                 int builtin, int status) {
            this.code = code;
            this.name = name;
            this.description = description;
            this.permissions = permissions;
            this.departmentScope = departmentScope;
            this.builtin = builtin;
            this.status = status;
        }
    }

    public static final class SeedDepartment {
        public final String code;
        public final String parentCode;
        public final String name;
        public final int sortOrder;
        public final String description;

        SeedDepartment(String code, String parentCode, String name, int sortOrder, String description) {
            this.code = code;
            this.parentCode = parentCode;
            this.name = name;
            this.sortOrder = sortOrder;
            this.description = description;
        }
    }

    public static final class SeedUser {
        public final String username;
        public final String displayName;
        /** 一个账号可以挂多个角色，权限取并集、数据范围取最宽。 */
        public final List<String> roleCodes;
        public final String departmentCode;
        public final String email;
        public final int status;
        public final String createdTime;
        public final String editTime;

        SeedUser(String username, String displayName, List<String> roleCodes, String departmentCode, String email,
                 int status, String createdTime, String editTime) {
            this.username = username;
            this.displayName = displayName;
            this.roleCodes = roleCodes;
            this.departmentCode = departmentCode;
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
        /** 接收人的 sys_user.id；0 表示广播，所有人可见。 */
        public final int recipientUserId;
        public final String createdTime;

        SeedNotification(String title, String description, int recipientUserId, String createdTime) {
            this.title = title;
            this.description = description;
            this.recipientUserId = recipientUserId;
            this.createdTime = createdTime;
        }
    }
}
