package tech.smartboot.feat.demo.plus.security;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import tech.smartboot.feat.demo.plus.common.DomainException;
import tech.smartboot.feat.demo.plus.model.AuthAccount;
import tech.smartboot.feat.demo.plus.model.AuthRole;

/**
 * 权限点字典。
 * <p>
 * 角色的 permissions 列存的是这些 key 的逗号拼接，后端每个写接口用 require(permission) 校验。
 * <p>
 * <b>这里是权限点的唯一真相。</b> 分组、中文名、说明都写在 {@link #CATALOG} 里，
 * {@link #ALL} 由它派生，前端通过 {@code GET /api/permissions} 取同一份字典来渲染角色页的勾选项。
 * 之前这套信息在 Java 与前端 {@code permissions.ts} 各维护一份，加一个权限点要改两处，
 * 而漏改前端的表现是「角色页里配不出这个点，接口永远 403」——不报错，只报障。
 * <p>
 * 注意：这里必须写成普通类的常量，不能放进 @Mapper 接口（会触发 AOT 处理的类型转换异常）。
 */
public final class Permission {

    public static final String DASHBOARD_READ = "dashboard:read";

    public static final String USER_READ = "user:read";
    public static final String USER_WRITE = "user:write";

    public static final String ROLE_READ = "role:read";
    public static final String ROLE_WRITE = "role:write";

    public static final String DEPT_READ = "dept:read";
    public static final String DEPT_WRITE = "dept:write";

    /** 审计日志：只读，能看谁在什么时候改了什么。刻意不给写权限——日志只能由系统写。 */
    public static final String AUDIT_READ = "audit:read";

    /**
     * 菜单配置。
     * <p>
     * 权限点本身仍然留在代码里，不进数据库：每个接口的 {@code require(...)} 是编译期写死的，
     * 允许运行时新增权限点只会造出「字典里有、接口从不校验」的空权限。可变的是「菜单挂哪个权限点」，
     * 不是「有哪些权限点」。
     */
    public static final String MENU_READ = "menu:read";
    public static final String MENU_WRITE = "menu:write";

    /** 重置演示数据这类系统级动作。 */
    public static final String SYSTEM_WRITE = "system:write";

    /**
     * 权限点全集，声明顺序即展示顺序。
     * <p>
     * 每条的 groupKey 同时被前端用来分组渲染，因此新增权限点时只需在这里加一行——
     * 前端不再有任何需要同步的常量。
     */
    public static final List<Entry> CATALOG = Collections.unmodifiableList(Arrays.asList(
            new Entry("dashboard", "数据概览", DASHBOARD_READ,
                    "查看概览", "访问首页仪表盘与登录趋势"),

            new Entry("user", "成员管理", USER_READ,
                    "查看成员", "查看成员列表与详情"),
            new Entry("user", "成员管理", USER_WRITE,
                    "管理成员", "新建、编辑、停用、删除与重置密码"),

            new Entry("role", "角色权限", ROLE_READ,
                    "查看角色", "查看角色列表与权限点"),
            new Entry("role", "角色权限", ROLE_WRITE,
                    "管理角色", "新建、编辑、停用与删除角色"),

            new Entry("dept", "部门管理", DEPT_READ,
                    "查看部门", "查看部门树与成员数"),
            new Entry("dept", "部门管理", DEPT_WRITE,
                    "管理部门", "新建、调整层级与删除部门"),

            new Entry("audit", "审计日志", AUDIT_READ,
                    "查看审计日志", "查看谁在什么时候改了谁，只读"),

            new Entry("system", "系统", MENU_READ,
                    "查看菜单", "查看侧边栏菜单配置"),
            new Entry("system", "系统", MENU_WRITE,
                    "管理菜单", "新增、修改、删除菜单项，调整分组与顺序"),

            new Entry("system", "系统", SYSTEM_WRITE,
                    "重置演示数据", "清空并重建全部演示数据")));

    /** 全部权限点的 key，顺序与 {@link #CATALOG} 一致。由 CATALOG 派生，不要单独维护。 */
    public static final List<String> ALL;

    static {
        List<String> keys = new ArrayList<>(CATALOG.size());
        for (Entry entry : CATALOG) {
            keys.add(entry.key);
        }
        ALL = Collections.unmodifiableList(keys);
    }

    private Permission() {
    }

    public static boolean isValid(String key) {
        return key != null && ALL.contains(key.trim());
    }

    /**
     * 把逗号分隔的文本拆成去重后的列表，顺序保持稳定，未知 key 直接丢弃。
     */
    public static List<String> split(String csv) {
        if (csv == null || csv.trim().isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> result = new LinkedHashSet<>();
        for (String part : csv.split(",")) {
            String key = part.trim();
            if (isValid(key)) {
                result.add(key);
            }
        }
        return new ArrayList<>(result);
    }

    /**
     * 校验并规整一组权限点：去重、按 ALL 的声明顺序排序，便于前端展示与后续 diff。
     */
    public static String join(List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return "";
        }
        Set<String> selected = new LinkedHashSet<>();
        for (String raw : keys) {
            String key = raw == null ? null : raw.trim();
            if (key == null || key.isEmpty()) {
                continue;
            }
            if (!isValid(key)) {
                throw new DomainException("未知的权限点：" + key);
            }
            selected.add(key);
        }
        StringBuilder builder = new StringBuilder();
        for (String key : ALL) {
            if (!selected.contains(key)) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(',');
            }
            builder.append(key);
        }
        return builder.toString();
    }

    public static boolean contains(List<String> keys, String permission) {
        return keys != null && permission != null && keys.contains(permission);
    }

    /**
     * 一个账号实际生效的权限点。
     * <ul>
     *   <li>任一角色是 ADMIN，就拥有全部权限——超管是兜底通道，即使角色表被改坏也不能把自己锁在门外；</li>
     *   <li>停用的角色整条失效，不贡献任何权限；</li>
     *   <li>没有任何角色时登录仍可用，但没有任何业务权限。</li>
     * </ul>
     * 放在这里而不是 {@link tech.smartboot.feat.demo.plus.model.AuthAccount}：合并要读本字典，
     * 写在实体上会让 model 反向依赖 security。
     */
    public static List<String> effective(AuthAccount account) {
        if (account.hasRole("ADMIN")) {
            return ALL;
        }
        Set<String> merged = new LinkedHashSet<>();
        for (AuthRole role : account.getRoles()) {
            if (role.isEnabled()) {
                merged.addAll(split(role.getPermissions()));
            }
        }
        if (merged.isEmpty()) {
            return Collections.emptyList();
        }
        // 按 ALL 的声明顺序返回，保证前端展示与角色页勾选顺序稳定。
        List<String> ordered = new ArrayList<>(merged.size());
        for (String key : ALL) {
            if (merged.contains(key)) {
                ordered.add(key);
            }
        }
        return ordered;
    }

    /**
     * 权限点的展示元数据。
     * <p>
     * 不是 DTO：不会被 AOT 序列化器处理，只在 security 与 service 之间传递，所以用 getter 而不用公开字段，
     * 与项目里其它值对象的写法保持一致。真正下发到前端的是 {@code dto.PermissionItemResponse}。
     */
    public static final class Entry {
        private final String groupKey;
        private final String groupLabel;
        private final String key;
        private final String label;
        private final String description;

        Entry(String groupKey, String groupLabel, String key, String label, String description) {
            this.groupKey = groupKey;
            this.groupLabel = groupLabel;
            this.key = key;
            this.label = label;
            this.description = description;
        }

        public String getGroupKey() {
            return groupKey;
        }

        public String getGroupLabel() {
            return groupLabel;
        }

        public String getKey() {
            return key;
        }

        public String getLabel() {
            return label;
        }

        public String getDescription() {
            return description;
        }
    }
}
