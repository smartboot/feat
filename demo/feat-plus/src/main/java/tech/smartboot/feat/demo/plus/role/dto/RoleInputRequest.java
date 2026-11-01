package tech.smartboot.feat.demo.plus.role.dto;

import java.util.List;

/**
 * 创建/更新角色的请求体，字段与前端 RoleInput 对齐。
 * 更新时忽略 code：编码是权限判定的锚点，改了等于换一个角色。
 */
public class RoleInputRequest {
    private String code;
    private String name;
    private String description;
    private List<String> permissions;
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

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
