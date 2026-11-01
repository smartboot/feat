package tech.smartboot.feat.demo.plus.auth.model;

/**
 * 账号身上的一个角色，只带鉴权需要的最小字段。
 * <p>
 * 一个账号可以挂多个角色，权限点取并集、数据范围取最宽（见 AccessGuard）。
 * status 为 0 的角色整条失效：既不贡献权限点，也不贡献数据范围。
 */
public class AuthRole {
    private String code;
    private String name;
    private int status;
    private String permissions;
    private int departmentScope;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getPermissions() {
        return permissions;
    }

    public void setPermissions(String permissions) {
        this.permissions = permissions;
    }

    public int getDepartmentScope() {
        return departmentScope;
    }

    public void setDepartmentScope(int departmentScope) {
        this.departmentScope = departmentScope;
    }

    public boolean isEnabled() {
        return status == 1;
    }
}
