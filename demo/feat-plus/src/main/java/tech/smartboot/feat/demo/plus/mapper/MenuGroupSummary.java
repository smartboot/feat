package tech.smartboot.feat.demo.plus.mapper;

/**
 * sys_menu 按分组聚合后的汇总行，供 {@link MenuMapper#selectGroups()} 使用。
 * <p>
 * 普通类而不是 @Mapper 接口内部类：注解处理器会把 @Mapper 接口里的所有元素强转
 * ExecutableElement，接口里出现嵌套类型同样会编译失败，所以汇总行必须独立成文件。
 * 列名与属性同名映射（group_key / group_label / group_sort / menu_count）。
 */
public class MenuGroupSummary {
    private String groupKey;
    private String groupLabel;
    private int groupSort;
    private long menuCount;

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
