package tech.smartboot.feat.demo.plus.department.dto;

/**
 * 创建/更新部门的请求体，字段与前端 DepartmentInput 对齐。
 * parentId 传 0 表示根部门。
 */
public class DepartmentInputRequest {
    private Long parentId;
    private String code;
    private String name;
    private Integer sortOrder;
    private String description;
    private Integer status;

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

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

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
