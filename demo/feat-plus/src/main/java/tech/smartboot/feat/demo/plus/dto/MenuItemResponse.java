package tech.smartboot.feat.demo.plus.dto;

/**
 * 一条菜单，随登录响应与 {@code GET /api/auth/me} 一起下发，不单独开接口。
 * <p>
 * 不单独开接口是有意的：多一次请求意味着菜单接口挂掉就白屏，首屏也多一次串行等待。
 * 菜单体积很小（当前 6 条），跟着当前登录人一起返回最省事。
 * <p>
 * 前端按 {@code groupKey} 分组，组内按 {@code sort} 排序；{@code visible} 为 false 的项只贡献
 * 「路径 → 标题」，不进侧栏（个人中心就是这种）。{@code permission} 为空串表示只要登录就能看——
 * 不用 null，因为登录响应更深一层、走的是 fastjson2 序列化，null 字段会被省略，
 * 同一个菜单在两个接口上的字段集合就会不一致。
 */
public class MenuItemResponse {
    private String groupKey;
    private String groupLabel;
    private String path;
    private String name;
    private String icon;
    private String permission;
    private int sort;
    private boolean visible;

    public MenuItemResponse() {
    }

    public MenuItemResponse(String groupKey, String groupLabel, String path, String name, String icon,
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

    public String getGroupKey() {
        return groupKey;
    }

    public void setGroupKey(String groupKey) {
        this.groupKey = groupKey;
    }

    public String getGroupLabel() {
        return groupLabel;
    }

    public void setGroupLabel(String groupLabel) {
        this.groupLabel = groupLabel;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public int getSort() {
        return sort;
    }

    public void setSort(int sort) {
        this.sort = sort;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }
}
