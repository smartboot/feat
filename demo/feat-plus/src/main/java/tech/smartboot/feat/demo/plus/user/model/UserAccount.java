package tech.smartboot.feat.demo.plus.user.model;

import java.util.Date;

/**
 * 用户账号档案，对应 sys_user 表。
 * <p>
 * 列表查询不会查 password_hash，此时该字段为 null，序列化时会被自动忽略。
 * departmentId 是外键，departmentName 来自 sys_department 的左连接，
 * 只用于展示与关键字搜索，写库时只认 departmentId。
 * <p>
 * 角色不在这张表里：一个人可以挂多个角色，关系存在 sys_user_role，
 * 由 UserService 在拿到用户行之后批量补齐（见 UserItemResponse）。
 * isDeleted / createdTime / editTime / creator / editor 是四张表统一要求的审计字段：
 * 删除只置 isDeleted，实体本身保留在表里，因此所有查询都必须带 is_deleted = 0 过滤。
 */
public class UserAccount {
    private long id;
    private String username;
    private String passwordHash;
    private String displayName;
    private Long departmentId;
    private String departmentName;
    private String email;
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

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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
