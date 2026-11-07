package tech.smartboot.feat.demo.plus.security;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 前端已经存在的页面清单——菜单的 {@code path} 只能取这里面的值。
 * <p>
 * <b>为什么需要这份白名单。</b> 菜单入库之后，路径变成可以在界面上随便填的文本框。填错不会报错，
 * 表现是「侧栏有这一条，点进去被 catch-all 弹回首页」——排查成本极高，因为数据看起来完全正常。
 * 有了白名单，新菜单只能在「确实存在的前端页面」里选，错误从运行时静默失败变成接口 400。
 * <p>
 * <b>为什么不能由后端直接扫描前端路由。</b> 路由里的 {@code element} 是编译进 bundle 的组件，
 * 后端拿不到；Feat 没有运行期反射去遍历注解，构建产物也不会带路由表。所以只能在两侧各留一份，
 * 由 {@code scripts/check_menu.py} 双向校验：这里每条路径在 {@code App.tsx} 里有 Route，
 * {@code App.tsx} 里每条 Route（除登录与兜底）也都在这里。漏改任一侧，脚本立刻失败。
 * <p>
 * 新增页面时：{@code App.tsx} 加 Route → 这里加一行 → 在菜单管理页（或种子数据）里加一条菜单。
 * 前两步漏了任何一步，check_menu.py 都会拦住；第三步漏了的表现是「页面能打开但没有入口」，
 * 脚本也会提示。
 */
public final class MenuRoutes {

    /** 数据概览。 */
    public static final String DASHBOARD = "/dashboard";
    /** 成员管理。 */
    public static final String USERS = "/users";
    /** 角色权限。 */
    public static final String ROLES = "/roles";
    /** 部门管理。 */
    public static final String DEPARTMENTS = "/departments";
    /** 审计日志。 */
    public static final String ACTIVITIES = "/activities";
    /** 菜单管理。 */
    public static final String MENUS = "/menus";
    /** 个人中心：不进侧栏，但仍是标题来源。 */
    public static final String PROFILE = "/profile";

    public static final List<String> ALL = Collections.unmodifiableList(Arrays.asList(
            DASHBOARD, USERS, ROLES, DEPARTMENTS, ACTIVITIES, MENUS, PROFILE));

    private MenuRoutes() {
    }

    public static boolean isValid(String path) {
        return path != null && ALL.contains(path.trim());
    }
}
