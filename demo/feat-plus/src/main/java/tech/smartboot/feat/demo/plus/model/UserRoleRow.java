package tech.smartboot.feat.demo.plus.model;

/**
 * 用户与角色的关联行，用于把当前页用户的角色一次性补齐。
 * <p>
 * 单独用一个类而不是 Map，是因为 MyBatis 的 mapUnderscoreToCamelCase 只对 POJO 生效。
 */
public class UserRoleRow {
    private long userId;
    private String roleCode;
    private String roleName;

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
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
}
