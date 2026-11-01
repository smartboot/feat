package tech.smartboot.feat.demo.plus.auth.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import tech.smartboot.feat.demo.plus.common.Permission;

/**
 * 登录态需要的最小账号视图：密码哈希 + 所属部门 + 挂在身上的角色。
 * <p>
 * 角色不再是一行 join 出来的单值，而是 sys_user_role 查出来的列表：
 * 一个人可以同时是「部门主管」和「审计员」，权限点取并集。
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
     * 实际生效的权限点：
     * <ul>
     *   <li>任一角色是 ADMIN，就拥有全部权限——超管是兜底通道，即使角色表被改坏也不能把自己锁在门外；</li>
     *   <li>停用的角色整条失效，不贡献任何权限；</li>
     *   <li>没有任何角色时登录仍可用，但没有任何业务权限。</li>
     * </ul>
     */
    public List<String> effectivePermissions() {
        if (hasRole("ADMIN")) {
            return Permission.ALL;
        }
        Set<String> merged = new LinkedHashSet<>();
        for (AuthRole role : roles) {
            if (role.isEnabled()) {
                merged.addAll(Permission.split(role.getPermissions()));
            }
        }
        if (merged.isEmpty()) {
            return Collections.emptyList();
        }
        // 按 Permission.ALL 的声明顺序返回，保证前端展示与角色页勾选顺序稳定。
        List<String> ordered = new ArrayList<>(merged.size());
        for (String key : Permission.ALL) {
            if (merged.contains(key)) {
                ordered.add(key);
            }
        }
        return ordered;
    }

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
