package tech.smartboot.feat.demo.plus.dto;

/**
 * 更新菜单分组的请求体，对应 {@code PUT /api/menus/groups/{groupKey}}。
 * <p>
 * groupKey 在 URL 路径里，请求体只有 label 与 sort。sort 允许缺省（null 当 0 处理），
 * 只改名字不动顺序是合法操作。
 */
public class GroupUpdateRequest {
    private String label;
    private Integer sort;

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }
}
