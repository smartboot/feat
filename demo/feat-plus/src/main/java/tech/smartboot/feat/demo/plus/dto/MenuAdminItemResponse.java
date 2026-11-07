package tech.smartboot.feat.demo.plus.dto;

/**
 * 菜单管理页的一行。与下发给侧栏的 {@link MenuItemResponse} 分开，是因为管理页还需要
 * {@code id}（改删要用）与 {@code groupSort}（调分组顺序要用），而这两样对渲染侧栏毫无用处。
 * <p>
 * 不合并成一个 DTO：合并后每次 {@code /me} 都会多输出两个字段，而 {@code /me} 是每个请求都走的路径。
 */
public class MenuAdminItemResponse {
    private long id;
    private String groupKey;
    private String groupLabel;
    private int groupSort;
    private String path;
    private String name;
    private String icon;
    private String permission;
    private int sort;
    private boolean visible;

    public MenuAdminItemResponse() {
    }

    public MenuAdminItemResponse(long id, String groupKey, String groupLabel, int groupSort, String path,
                                 String name, String icon, String permission, int sort, boolean visible) {
        this.id = id;
        this.groupKey = groupKey;
        this.groupLabel = groupLabel;
        this.groupSort = groupSort;
        this.path = path;
        this.name = name;
        this.icon = icon;
        this.permission = permission;
        this.sort = sort;
        this.visible = visible;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
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

    public int getGroupSort() {
        return groupSort;
    }

    public void setGroupSort(int groupSort) {
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
