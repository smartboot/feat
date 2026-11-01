package tech.smartboot.feat.demo.plus.role.model;

import java.util.Date;

/**
 * 角色行记录。
 * <p>
 * permissions 在库里是一列逗号分隔的文本，读写都走字符串，
 * 拆分成集合的动作统一放在 RolePermissions 与 DTO 层，避免模型里出现集合字段
 * 导致 MyBatis 与 JSON 序列化两边语义不一致。
 */
public class RoleRecord {
    private long id;
    private String code;
    private String name;
    private String description;
    private String permissions;
    /**
     * 数据范围档位，取值见 {@link tech.smartboot.feat.demo.plus.common.DataScope}。
     * 只影响「能看到谁」，不影响「能不能做」——后者由 permissions 决定。
     */
    private int departmentScope;
    private int builtin;
    private int status;
    private int isDeleted;
    private Date createdTime;
    private Date editTime;
    private Long creator;
    private Long editor;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public int getBuiltin() {
        return builtin;
    }

    public void setBuiltin(int builtin) {
        this.builtin = builtin;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(int isDeleted) {
        this.isDeleted = isDeleted;
    }

    public Date getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(Date createdTime) {
        this.createdTime = createdTime;
    }

    public Date getEditTime() {
        return editTime;
    }

    public void setEditTime(Date editTime) {
        this.editTime = editTime;
    }

    public Long getCreator() {
        return creator;
    }

    public void setCreator(Long creator) {
        this.creator = creator;
    }

    public Long getEditor() {
        return editor;
    }

    public void setEditor(Long editor) {
        this.editor = editor;
    }
}
