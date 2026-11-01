package tech.smartboot.feat.demo.plus.security;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 菜单注册表：侧边栏与页面标题的唯一来源。
 * <p>
 * <b>为什么菜单放在后端。</b> 原来菜单硬编码在前端三个地方（{@code primaryNav}、{@code systemNav}、
 * {@code PAGE_TITLES}），加一个模块要改三处，漏一处不报错：菜单不出现但 URL 能打开，或者点进去被
 * catch-all 弹回首页。放到后端之后只剩这一份定义，前端只负责「给定 path 渲染哪个组件」。
 * <p>
 * <b>为什么这里只有元数据，没有路由。</b> Feat 的路由在编译期确定，后端最多只能下发 path 字符串，
 * 拿不到要渲染的组件。所以边界是：路径 → 标题 → 图标 → 权限点归后端，组件归前端编译产物。
 * 两侧一致性由 {@code scripts/check_menu.py} 兜住——注册表里的 path 必须在前端路由里存在。
 * <p>
 * 新增模块时：在这里加一行，删掉前端该模块的菜单登记。前端仍需在 {@code App.tsx} 登记一条 Route，
 * 那是组件映射，不是菜单。
 */
public final class MenuRegistry {

    private static final List<MenuItem> ITEMS = Collections.unmodifiableList(java.util.Arrays.asList(
            // 工作台
            new MenuItem("workspace", "工作台", "/dashboard", "数据概览", "dashboard",
                    Permission.DASHBOARD_READ, 10, true),
            new MenuItem("workspace", "工作台", "/users", "用户管理", "users",
                    Permission.USER_READ, 20, true),

            // 系统管理
            new MenuItem("system", "系统管理", "/roles", "角色权限", "shield",
                    Permission.ROLE_READ, 10, true),
            new MenuItem("system", "系统管理", "/departments", "部门管理", "building",
                    Permission.DEPT_READ, 20, true),
            new MenuItem("system", "系统管理", "/activities", "审计日志", "scroll",
                    Permission.AUDIT_READ, 30, true),

            // 账号：不进侧栏，但标题仍要从同一份注册表来，否则「当前页叫什么」又会出现第二个来源。
            // permission 传空串表示不要求权限点——不用 null，原因见 MenuItem#permission。
            new MenuItem("account", "账号", "/profile", "个人中心", "user",
                    "", 10, false)));

    private MenuRegistry() {
    }

    /**
     * 当前登录人可见的菜单。
     * <p>
     * 只按权限点过滤，不做数据范围过滤——数据范围决定「能看到哪些行」，不决定「能进哪个页面」。
     * 这个过滤只是让界面干净，真正的拦截在 {@link AccessGuard#require}：漏了这里最多是菜单多一条，
     * 点进去仍然 403。
     */
    public static List<MenuItem> visibleFor(List<String> permissions) {
        List<MenuItem> result = new ArrayList<>(ITEMS.size());
        for (MenuItem item : ITEMS) {
            if (item.permission.isEmpty() || Permission.contains(permissions, item.permission)) {
                result.add(item);
            }
        }
        return result;
    }

    /** 全部定义，供静态检查脚本与断言使用。 */
    public static List<MenuItem> all() {
        return ITEMS;
    }
}
