package tech.smartboot.feat.demo.plus.dto;

import java.util.List;

import tech.smartboot.feat.demo.plus.common.DateFormats;
import tech.smartboot.feat.demo.plus.model.ProfileRecord;

/**
 * 「我的资料」返回体。
 * <p>
 * 比 CurrentUserResponse 多出 email / departmentName / createdTime，少掉 permissions——
 * 个人中心不需要权限点，那是菜单与按钮用的。角色与数据范围仍复用 CurrentUserResponse 的结果，
 * 避免「个人中心显示的范围」和「实际生效的范围」两处各算一遍、算法漂移。
 */
public class ProfileResponse {
    private long id;
    private String username;
    private String displayName;
    private String email;
    private String departmentName;
    private List<String> roleNames;
    private String scopeDescription;
    private String createdTime;

    public static ProfileResponse from(ProfileRecord record, CurrentUserResponse current) {
        ProfileResponse response = new ProfileResponse();
        response.id = record.getId();
        response.username = record.getUsername();
        response.displayName = record.getDisplayName();
        response.email = record.getEmail();
        response.departmentName = record.getDepartmentName();
        response.roleNames = current.getRoleNames();
        response.scopeDescription = current.getScopeDescription();
        response.createdTime = DateFormats.format(record.getCreatedTime());
        return response;
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

    public String getEmail() {
        return email;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public List<String> getRoleNames() {
        return roleNames;
    }

    public String getScopeDescription() {
        return scopeDescription;
    }

    public String getCreatedTime() {
        return createdTime;
    }
}
