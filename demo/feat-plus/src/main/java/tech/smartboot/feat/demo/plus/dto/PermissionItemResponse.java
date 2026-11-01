package tech.smartboot.feat.demo.plus.dto;

/**
 * 一个权限点及其展示信息，由 {@code GET /api/permissions} 下发。
 * <p>
 * 前端拿它渲染角色页的勾选项（分组、中文名、说明），不再自己维护一份字典。
 * 之前那份 {@code permissions.ts} 与后端 {@code Permission} 各写一遍，加权限点要改两处，
 * 漏改前端的结果是「角色页里配不出这个点，接口永远 403」——不报错，只报障。
 * <p>
 * 注意这里返回的是<b>字典</b>而不是「当前登录人拥有的权限」：角色页要能给角色授予自己都没有的权限点，
 * 所以不能从 {@code CurrentUserResponse.permissions} 推导。
 */
public class PermissionItemResponse {
    private String groupKey;
    private String groupLabel;
    private String key;
    private String label;
    private String description;
    private int sort;

    public PermissionItemResponse() {
    }

    public PermissionItemResponse(String groupKey, String groupLabel, String key, String label,
                                  String description, int sort) {
        this.groupKey = groupKey;
        this.groupLabel = groupLabel;
        this.key = key;
        this.label = label;
        this.description = description;
        this.sort = sort;
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

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getSort() {
        return sort;
    }

    public void setSort(int sort) {
        this.sort = sort;
    }
}
