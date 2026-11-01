package tech.smartboot.feat.demo.plus.dto;

import java.util.Collections;
import java.util.List;

import tech.smartboot.feat.demo.plus.common.DateFormats;
import tech.smartboot.feat.demo.plus.model.UserAccount;

/**
 * 用户列表项，字段与前端 UserListItem 对齐。
 * 时间以文本形式返回，格式为 yyyy-MM-dd HH:mm。
 * <p>
 * 角色返回两个平行数组：roleCodes 用于提交与筛选，roleNames 用于展示。
 * 分开而不是返回对象数组，是因为前端 90% 的地方只需要名字拼串或编码比对，
 * 而对象数组会让「按角色筛选」这类简单逻辑变啰嗦。
 * <p>
 * creator / editor 是操作人的用户 id（系统初始化数据为 null），isDeleted 不出现在响应里：
 * 被删除的行已被查询过滤掉，暴露这个字段只会让前端多一个恒为 false 的分支。
 */
public class UserItemResponse {
    private long id;
    private String username;
    private String displayName;
    private List<String> roleCodes;
    private List<String> roleNames;
    private Long departmentId;
    private String departmentName;
    private String email;
    private int status;
    private String createdTime;
    private String editTime;
    private Long creator;
    private Long editor;

    public static UserItemResponse from(UserAccount account, List<String> roleCodes, List<String> roleNames) {
        UserItemResponse item = new UserItemResponse();
        item.id = account.getId();
        item.username = account.getUsername();
        item.displayName = account.getDisplayName();
        item.roleCodes = roleCodes == null ? Collections.<String>emptyList() : roleCodes;
        item.roleNames = roleNames == null ? Collections.<String>emptyList() : roleNames;
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

    public List<String> getRoleCodes() {
        return roleCodes;
    }

    public List<String> getRoleNames() {
        return roleNames;
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
}
