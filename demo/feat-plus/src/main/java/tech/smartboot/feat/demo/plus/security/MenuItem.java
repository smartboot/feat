package tech.smartboot.feat.demo.plus.security;

/**
 * 一条菜单的定义。
 * <p>
 * 不是 DTO：不会被 AOT 序列化器处理，只在 security 与 service 之间传递，所以直接开公开字段，
 * 省掉一份纯粹为了搬运的 getter 样板。下发给前端的是 {@code dto.MenuItemResponse}。
 */
public final class MenuItem {

    /** 分组标识，前端按它把菜单归堆，顺序取分组第一次出现的先后。 */
    public final String groupKey;

    /** 分组中文名，例如「工作台」。 */
    public final String groupLabel;

    /** 前端路由路径。必须能在 {@code App.tsx} 里找到同名 Route，由 scripts/check_menu.py 校验。 */
    public final String path;

    /** 菜单名，同时是浏览器标签页的标题。 */
    public final String name;

    /** 图标 key，前端 NAV_ICONS 里按它取组件；找不到时退化为默认图标，不报错。 */
    public final String icon;

    /**
     * 需要的权限点；<b>空串表示只要登录就能看到</b>。
     * <p>
     * 不用 null：登录响应比 {@code /api/auth/me} 多一层嵌套（RestResult → LoginResponse → CurrentUser → Menu），
     * AOT 生成器在第三层会退化成 fastjson2 序列化，而 fastjson2 默认省略 null 字段——
     * 结果同一个菜单在登录响应里没有 permission 这个 key，在 /me 里是 {@code "permission": null}。
     * 空串能让两条路径的字段集合完全一致，比「前端小心处理 undefined」便宜得多。
     */
    public final String permission;

    /** 组内与组间排序，小的在前。 */
    public final int sort;

    /** 是否出现在侧边栏。false 的项只贡献「路径 → 标题」这一条映射，例如个人中心。 */
    public final boolean visible;

    public MenuItem(String groupKey, String groupLabel, String path, String name, String icon,
                    String permission, int sort, boolean visible) {
        this.groupKey = groupKey;
        this.groupLabel = groupLabel;
        this.path = path;
        this.name = name;
        this.icon = icon;
        this.permission = permission;
        this.sort = sort;
        this.visible = visible;
    }
}
