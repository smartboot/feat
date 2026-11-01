package tech.smartboot.feat.demo.plus.dashboard.model;

import java.util.Date;

/**
 * 站内通知，对应 sys_notification 表。
 * <p>
 * userId 是接收人：NULL 或 0 表示广播，大于 0 表示只投递给该用户。
 * <p>
 * readFlag 不是表里的列，而是查询时联 sys_notification_read 算出来的别名（1 表示当前用户已读）。
 * 保留它的 getter/setter 是因为 MyBatis 按属性名做映射，删掉 setter 就读不出这个别名了。
 */
public class NotificationRecord {
    private long id;
    private String title;
    private String description;
    private Long userId;
    private int readFlag;

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
