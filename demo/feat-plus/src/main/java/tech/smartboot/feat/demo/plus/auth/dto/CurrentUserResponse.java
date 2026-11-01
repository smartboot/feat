package tech.smartboot.feat.demo.plus.auth.dto;

import java.util.List;

/**
 * 当前登录人。
 * <p>
 * permissions 是全部角色合并后的权限点，前端据此决定菜单与按钮是否出现；
 * 后端每个接口仍会独立校验，前端的隐藏只是体验，不是安全边界。
 * <p>
 * 角色是数组：一个人可能同时是部门主管和审计员。roleCodes 按角色 id 排序，
 * 第一个可以当作「主角色」用于展示。
 * <p>
 * dataScope 是数据范围档位（1 全部 / 2 自定义 / 3 本部门 / 4 本部门及以下 / 5 仅本人），
 * scopeDescription 是给用户看的说明，例如「本部门及以下（3 个部门）」。
 * 前端拿它解释「为什么我只看到这些人」，而不是让用户以为是 bug。
 */
public class CurrentUserResponse {
    private long id;
    private String username;
    private String displayName;
    private List<String> roleCodes;
    private List<String> roleNames;
    private List<String> permissions;
    private int dataScope;
    private String scopeDescription;

    public CurrentUserResponse() {
    }

    public CurrentUserResponse(long id, String username, String displayName, List<String> roleCodes,
                               List<String> roleNames, List<String> permissions, int dataScope,
                               String scopeDescription) {
        this.id = id;
        this.username = username;
        this.displayName = displayName;
        this.roleCodes = roleCodes;
        this.roleNames = roleNames;
        this.permissions = permissions;
        this.dataScope = dataScope;
        this.scopeDescription = scopeDescription;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public List<String> getRoleCodes() {
        return roleCodes;
    }

    public void setRoleCodes(List<String> roleCodes) {
        this.roleCodes = roleCodes;
    }

    public List<String> getRoleNames() {
        return roleNames;
    }

    public void setRoleNames(List<String> roleNames) {
        this.roleNames = roleNames;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }

    public int getDataScope() {
        return dataScope;
    }

    public void setDataScope(int dataScope) {
        this.dataScope = dataScope;
    }

    public String getScopeDescription() {
        return scopeDescription;
    }

    public void setScopeDescription(String scopeDescription) {
        this.scopeDescription = scopeDescription;
    }
}
