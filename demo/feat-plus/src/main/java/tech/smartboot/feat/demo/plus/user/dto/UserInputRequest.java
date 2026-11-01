package tech.smartboot.feat.demo.plus.user.dto;

/**
 * 创建/更新用户的请求体，字段与前端 UserInput 对齐。
 * <p>
 * 部门传的是 sys_department.id，不再传部门名称文本：
 * 名称只是展示用的副本，改名后不该留在用户表里变成历史遗留。
 */
public class UserInputRequest {
    private String username;
    private String displayName;
    private String roleCode;
    private Long departmentId;
    private String email;
    private Integer status;

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

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
