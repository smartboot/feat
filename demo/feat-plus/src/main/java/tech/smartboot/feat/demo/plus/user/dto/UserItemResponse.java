package tech.smartboot.feat.demo.plus.user.dto;

import java.util.Date;

import tech.smartboot.feat.demo.plus.common.DateFormats;
import tech.smartboot.feat.demo.plus.user.model.UserAccount;

/**
 * 用户列表项，字段与前端 UserListItem 对齐。
 * 时间以文本形式返回，格式与前端 mock 一致（yyyy-MM-dd HH:mm）。
 * <p>
 * creator / editor 是操作人的用户 id（系统初始化数据为 null），isDeleted 不出现在响应里：
 * 被删除的行已被查询过滤掉，暴露这个字段只会让前端多一个恒为 false 的分支。
 */
public class UserItemResponse {
    private long id;
    private String username;
    private String displayName;
    private String roleCode;
    private Long departmentId;
    private String departmentName;
    private String email;
    private int status;
    private String createdTime;
    private String editTime;
    private Long creator;
    private Long editor;

    public static UserItemResponse from(UserAccount account) {
        UserItemResponse item = new UserItemResponse();
        item.id = account.getId();
        item.username = account.getUsername();
        item.displayName = account.getDisplayName();
        item.roleCode = account.getRoleCode();
        item.departmentId = account.getDepartmentId();
        item.departmentName = account.getDepartmentName();
        item.email = account.getEmail();
        item.status = account.getStatus();
        item.createdTime = DateFormats.format(account.getCreatedTime());
        item.editTime = DateFormats.format(account.getEditTime());
        item.creator = account.getCreator();
        item.editor = account.getEditor();
        return item;
    }

    public long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public String getEmail() {
        return email;
    }

    public int getStatus() {
        return status;
    }

    public String getCreatedTime() {
        return createdTime;
    }

    public String getEditTime() {
        return editTime;
    }

    public Long getCreator() {
        return creator;
    }

    public Long getEditor() {
        return editor;
    }

    /**
     * 仅用于测试与调试时构造固定时间。
     */
    static String formatOrNull(Date date) {
        return DateFormats.format(date);
    }
}
