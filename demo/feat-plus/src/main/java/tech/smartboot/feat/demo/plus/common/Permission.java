package tech.smartboot.feat.demo.plus.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 权限点字典。
 * <p>
 * 角色的 permissions 列存的是这些 key 的逗号拼接，后端每个写接口用 require(permission) 校验，
 * 前端用同一份 key 决定菜单和按钮是否可见。
 * 新增功能时先在这里登记，否则无法在角色页勾选、也无法被校验。
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

    /** 重置演示数据这类系统级动作。 */
    public static final String SYSTEM_WRITE = "system:write";

    public static final List<String> ALL = Collections.unmodifiableList(Arrays.asList(
            DASHBOARD_READ,
            USER_READ, USER_WRITE,
            ROLE_READ, ROLE_WRITE,
            DEPT_READ, DEPT_WRITE,
            SYSTEM_WRITE));

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
}
