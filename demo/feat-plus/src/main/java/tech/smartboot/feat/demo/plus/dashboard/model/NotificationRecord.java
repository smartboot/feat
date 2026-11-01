package tech.smartboot.feat.demo.plus.dashboard.model;

import java.util.Date;

/**
 * 站内通知，对应 sys_notification 表。
 * <p>
 * 列名是 read_flag（read 是 SQL 关键字），字段名与下划线转驼峰规则保持一致。
 */
public class NotificationRecord {
    private long id;
    private String title;
    private String description;
    private Long userId;
    private int readFlag;
    private Date createdAt;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public int getReadFlag() {
        return readFlag;
    }

    public void setReadFlag(int readFlag) {
        this.readFlag = readFlag;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}
