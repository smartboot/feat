package tech.smartboot.feat.demo.plus.role.dto;

import java.util.List;

import tech.smartboot.feat.demo.plus.common.DateFormats;
import tech.smartboot.feat.demo.plus.common.Permission;
import tech.smartboot.feat.demo.plus.role.model.RoleRecord;

/**
 * 角色列表项，字段与前端 RoleItem 对齐。
 * permissions 拆成数组返回，前端直接渲染成权限标签。
 */
public class RoleItemResponse {
    private long id;
    private String code;
    private String name;
    private String description;
    private List<String> permissions;
    private boolean builtin;
    private int status;
    private long userCount;
    private String createdTime;
    private String editTime;

    public static RoleItemResponse from(RoleRecord role, long userCount) {
        RoleItemResponse item = new RoleItemResponse();
        item.id = role.getId();
        item.code = role.getCode();
        item.name = role.getName();
        item.description = role.getDescription();
        item.permissions = Permission.split(role.getPermissions());
        item.builtin = role.getBuiltin() == 1;
        item.status = role.getStatus();
        item.userCount = userCount;
        item.createdTime = DateFormats.format(role.getCreatedTime());
        item.editTime = DateFormats.format(role.getEditTime());
        return item;
    }

    public long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public boolean isBuiltin() {
        return builtin;
    }

    public int getStatus() {
        return status;
    }

    public long getUserCount() {
        return userCount;
    }

    public String getCreatedTime() {
        return createdTime;
    }

    public String getEditTime() {
        return editTime;
    }
}
