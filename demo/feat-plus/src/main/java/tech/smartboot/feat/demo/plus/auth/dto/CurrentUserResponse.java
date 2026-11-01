package tech.smartboot.feat.demo.plus.auth.dto;

import java.util.List;

/**
 * 当前登录人。permissions 是角色生效的权限点，前端据此决定菜单与按钮是否出现；
 * 后端每个接口仍会独立校验，前端的隐藏只是体验，不是安全边界。
 */
public class CurrentUserResponse {
    private long id;
    private String username;
    private String displayName;
    private String roleCode;
    private String roleName;
    private List<String> permissions;

    public CurrentUserResponse() {
    }

    public CurrentUserResponse(long id, String username, String displayName, String roleCode,
                               String roleName, List<String> permissions) {
        this.id = id;
        this.username = username;
        this.displayName = displayName;
        this.roleCode = roleCode;
        this.roleName = roleName;
        this.permissions = permissions;
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

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }
}
