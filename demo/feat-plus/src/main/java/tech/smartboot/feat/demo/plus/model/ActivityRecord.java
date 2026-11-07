package tech.smartboot.feat.demo.plus.model;

import java.util.Date;

/**
 * 操作动态，对应 sys_activity 表。
 * <p>
 * userId 表示这条动态「发生在谁身上」，creator 表示「谁做的」，两者可以不同。
 */
public class ActivityRecord {
    private long id;
    private String kind;
    private String title;
    private String description;
    private Long userId;
    /**
     * 操作人姓名，不是 sys_activity 的列，而是查询时联 sys_user 得到的别名。
     * 日志页要显示「谁做的」，而表里只有 creator 这个 id。
     */
    private String operatorName;

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

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
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

    public String getOperatorName() {
        return operatorName;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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
