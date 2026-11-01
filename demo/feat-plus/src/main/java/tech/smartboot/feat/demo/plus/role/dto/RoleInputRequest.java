package tech.smartboot.feat.demo.plus.role.dto;

import java.util.List;

/**
 * 创建/更新角色的请求体，字段与前端 RoleInput 对齐。
 * 更新时忽略 code：编码是权限判定的锚点，改了等于换一个角色。
 * <p>
 * departmentScope 是数据范围档位（1 全部 / 2 自定义 / 3 本部门 / 4 本部门及以下 / 5 仅本人），
 * departmentIds 只在「自定义」档位下有意义，其余档位会被忽略。
 */
public class RoleInputRequest {
    private String code;
    private String name;
    private String description;
    private List<String> permissions;
    private Integer departmentScope;
    private List<Long> departmentIds;
    private Integer status;

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }

    public Integer getDepartmentScope() {
        return departmentScope;
    }

    public void setDepartmentScope(Integer departmentScope) {
        this.departmentScope = departmentScope;
    }

    public List<Long> getDepartmentIds() {
        return departmentIds;
    }

    public void setDepartmentIds(List<Long> departmentIds) {
        this.departmentIds = departmentIds;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
