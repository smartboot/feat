package tech.smartboot.feat.demo.plus.user.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 创建/更新用户的请求体，字段与前端 UserInput 对齐。
 * <p>
 * 部门传的是 sys_department.id，不传部门名称文本：
 * 名称只是展示用的副本，改名后不该留在用户表里变成历史遗留。
 * <p>
 * 角色是数组：一个人可以同时是部门主管和审计员。
 * roleCode 是升级前的单角色字段，保留它只为兼容旧调用（脚本、第三方集成），
 * 两者同时出现时以 roleCodes 为准。
 */
public class UserInputRequest {
    private String username;
    private String displayName;
    private List<String> roleCodes;
    private String roleCode;
    private Long departmentId;
    private String email;
    private Integer status;

    /**
     * 规整后的角色编码列表：去重、去空。
     * 多个角色决定权限并集与数据范围，顺序不重要，但不能为空。
     */
    public List<String> effectiveRoleCodes() {
        Set<String> codes = new LinkedHashSet<>();
        if (roleCodes != null) {
            for (String code : roleCodes) {
                if (code != null && !code.trim().isEmpty()) {
                    codes.add(code.trim());
                }
            }
        }
        if (codes.isEmpty() && roleCode != null && !roleCode.trim().isEmpty()) {
            codes.add(roleCode.trim());
        }
        return codes.isEmpty() ? Collections.<String>emptyList() : new ArrayList<>(codes);
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
