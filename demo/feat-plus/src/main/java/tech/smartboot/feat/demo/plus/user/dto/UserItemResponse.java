package tech.smartboot.feat.demo.plus.user.dto;

import java.util.Date;

import tech.smartboot.feat.demo.plus.common.DateFormats;
import tech.smartboot.feat.demo.plus.user.model.UserAccount;

/**
 * 用户列表项，字段与前端 UserListItem 对齐。
 * 时间以文本形式返回，格式与前端 mock 一致（yyyy-MM-dd HH:mm）。
 */
public class UserItemResponse {
    private long id;
    private String username;
    private String displayName;
    private String roleCode;
    private String department;
    private String email;
    private int status;
    private String createdAt;
    private String updatedAt;

    public static UserItemResponse from(UserAccount account) {
        UserItemResponse item = new UserItemResponse();
        item.id = account.getId();
        item.username = account.getUsername();
        item.displayName = account.getDisplayName();
        item.roleCode = account.getRoleCode();
        item.department = account.getDepartment();
        item.email = account.getEmail();
        item.status = account.getStatus();
        item.createdAt = DateFormats.format(account.getCreatedAt());
        item.updatedAt = DateFormats.format(account.getUpdatedAt());
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

    public String getDepartment() {
        return department;
    }

    public String getEmail() {
        return email;
    }

    public int getStatus() {
        return status;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 仅用于测试与调试时构造固定时间。
     */
    static String formatOrNull(Date date) {
        return DateFormats.format(date);
    }
}
