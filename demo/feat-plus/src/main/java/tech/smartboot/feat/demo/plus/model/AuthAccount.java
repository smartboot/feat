package tech.smartboot.feat.demo.plus.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 登录态需要的最小账号视图：密码哈希 + 所属部门 + 挂在身上的角色。
 * <p>
 * 角色不再是一行 join 出来的单值，而是 sys_user_role 查出来的列表：
 * 一个人可以同时是「部门主管」和「审计员」，权限点取并集。
 * <p>
 * 这里刻意只放数据与「角色是否启用」这类自身判断，不放权限合并规则：
 * 合并要读 {@link tech.smartboot.feat.demo.plus.security.Permission} 字典，
 * 一旦写在这，model 就反向依赖 security，表结构层再也没法单独抽走。
 */
public class AuthAccount {
    private long id;
    private String username;
    private String passwordHash;
    private String displayName;
    private Long departmentId;
    private int status;
    private List<AuthRole> roles = Collections.emptyList();

    /**
     * 生效的角色（停用的不算）。顺序按角色 id，展示时第一个就是「主角色」。
     */
    public List<AuthRole> enabledRoles() {
        List<AuthRole> result = new ArrayList<>();
        for (AuthRole role : roles) {
            if (role.isEnabled()) {
                result.add(role);
            }
        }
        return result;
    }

    public boolean hasRole(String code) {
        for (AuthRole role : roles) {
            if (role.isEnabled() && code.equals(role.getCode())) {
                return true;
            }
        }
        return false;
    }

    public List<String> roleCodes() {
        List<String> codes = new ArrayList<>();
        for (AuthRole role : enabledRoles()) {
            codes.add(role.getCode());
        }
        return codes;
    }

    public List<String> roleNames() {
        List<String> names = new ArrayList<>();
        for (AuthRole role : enabledRoles()) {
            names.add(role.getName());
        }
        return names;
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

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public List<AuthRole> getRoles() {
        return roles;
    }

    public void setRoles(List<AuthRole> roles) {
        this.roles = roles == null ? Collections.<AuthRole>emptyList() : roles;
    }
}
