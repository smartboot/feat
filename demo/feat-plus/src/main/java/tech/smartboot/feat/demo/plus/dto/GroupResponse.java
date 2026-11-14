package tech.smartboot.feat.demo.plus.dto;

/**
 * 菜单分组的管理页展示项，来自 {@code GET /api/menus/groups}。
 * <p>
 * 聚合自 sys_menu：groupKey / groupLabel / groupSort 取每组第一条（同组内一致，写路径保证），
 * menuCount 是该组下的菜单数——0 表示空组。
 * <p>
 * 字段语义与前端 MenuGroup 一一对应（key/label/sort/count），分组渲染共用 {@code groupByKey}。
 */
public class GroupResponse {
    private String groupKey;
    private String groupLabel;
    private int groupSort;
    private long menuCount;

    public GroupResponse() {
    }

    public GroupResponse(String groupKey, String groupLabel, int groupSort, long menuCount) {
        this.groupKey = groupKey;
        this.groupLabel = groupLabel;
        this.groupSort = groupSort;
        this.menuCount = menuCount;
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

    public long getMenuCount() {
        return menuCount;
    }

    public void setMenuCount(long menuCount) {
        this.menuCount = menuCount;
    }
}
