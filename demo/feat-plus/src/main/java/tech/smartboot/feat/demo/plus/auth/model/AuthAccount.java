package tech.smartboot.feat.demo.plus.auth.model;

import java.util.Collections;
import java.util.List;

import tech.smartboot.feat.demo.plus.common.Permission;

/**
 * 登录态需要的最小账号视图：密码哈希 + 角色 + 该角色生效的权限点。
 * <p>
 * permissions / roleStatus 来自 sys_role 的左连接，角色被删或不存在时为 null。
 */
public class AuthAccount {
    private long id;
    private String username;
    private String passwordHash;
    private String displayName;
    private String roleCode;
    private String roleName;
    private int status;
    private Integer roleStatus;
    private String permissions;

    /**
     * 实际生效的权限点：
     * <ul>
     *   <li>ADMIN 恒定拥有全部权限——超管是兜底通道，即使角色表被改坏也不能把自己锁在门外；</li>
     *   <li>角色被停用或已删除时，登录仍然可用，但没有任何业务权限。</li>
     * </ul>
     */
    public List<String> effectivePermissions() {
        if ("ADMIN".equals(roleCode)) {
            return Permission.ALL;
        }
        if (roleStatus == null || roleStatus != 1) {
            return Collections.emptyList();
        }
        return Permission.split(permissions);
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

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
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

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public Integer getRoleStatus() {
        return roleStatus;
    }

    public void setRoleStatus(Integer roleStatus) {
        this.roleStatus = roleStatus;
    }

    public String getPermissions() {
        return permissions;
    }

    public void setPermissions(String permissions) {
        this.permissions = permissions;
    }
}
