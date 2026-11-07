package tech.smartboot.feat.demo.plus.dto;

/**
 * 创建 / 更新菜单的请求体。
 * <p>
 * {@code path} 只能取 {@code security/MenuRoutes} 白名单里的值，由 MenuService 校验——
 * 允许自由填写会配出「菜单能点、页面不存在」，而这种错误在数据上完全看不出来。
 */
public class MenuInputRequest {
    private String groupKey;
    private String groupLabel;
    private Integer groupSort;
    private String path;
    private String name;
    private String icon;
    /** 空串表示登录即可见；非空必须是一个已登记的权限点。 */
    private String permission;
    private Integer sort;
    private Boolean visible;

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

    public Integer getGroupSort() {
        return groupSort;
    }

    public void setGroupSort(Integer groupSort) {
        this.groupSort = groupSort;
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

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }

    public Boolean getVisible() {
        return visible;
    }

    public void setVisible(Boolean visible) {
        this.visible = visible;
    }
}
