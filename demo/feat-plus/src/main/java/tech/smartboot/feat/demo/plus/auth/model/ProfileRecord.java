package tech.smartboot.feat.demo.plus.auth.model;

import java.util.Date;

/**
 * 个人中心要展示的账号信息。
 * <p>
 * 与 {@link AuthAccount} 的区别：AuthAccount 是登录用的最小集合（密码哈希、状态、角色），
 * 这里多带部门名称与创建时间，供「我的资料」页面直接渲染，不用前端再拼一次部门接口。
 */
public class ProfileRecord {

    private long id;
    private String username;
    private String displayName;
    private String email;
    private Long departmentId;
    private String departmentName;
    private int status;
    private Date createdTime;

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

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public Date getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(Date createdTime) {
        this.createdTime = createdTime;
    }
}
