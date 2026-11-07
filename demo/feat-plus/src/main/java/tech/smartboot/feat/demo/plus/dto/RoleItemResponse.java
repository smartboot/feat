package tech.smartboot.feat.demo.plus.dto;

import java.util.Collections;
import java.util.List;

import tech.smartboot.feat.demo.plus.common.DateFormats;
import tech.smartboot.feat.demo.plus.model.RoleRecord;
import tech.smartboot.feat.demo.plus.security.DataScope;
import tech.smartboot.feat.demo.plus.security.Permission;

/**
 * 角色列表项，字段与前端 RoleItem 对齐。
 * permissions 拆成数组返回，前端直接渲染成权限标签。
 * <p>
 * departmentScope 是数据范围档位，scopeLabel 是给人看的名称；
 * departmentIds 只在「自定义」档位下有值，其他档位返回空数组。
 */
public class RoleItemResponse {
    private long id;
    private String code;
    private String name;
    private String description;
    private List<String> permissions;
    private int departmentScope;
    private String scopeLabel;
    private List<Long> departmentIds;
    private boolean builtin;
    private int status;
    private long userCount;
    private String createdTime;
    private String editTime;

    public static RoleItemResponse from(RoleRecord role, long userCount, List<Long> departmentIds) {
        RoleItemResponse item = new RoleItemResponse();
        item.id = role.getId();
        item.code = role.getCode();
        item.name = role.getName();
        item.description = role.getDescription();
        item.permissions = Permission.split(role.getPermissions());
        item.departmentScope = DataScope.normalize(role.getDepartmentScope());
        item.scopeLabel = DataScope.label(item.departmentScope);
        item.departmentIds = departmentIds == null ? Collections.<Long>emptyList() : departmentIds;
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

    public int getDepartmentScope() {
        return departmentScope;
    }

    public String getScopeLabel() {
        return scopeLabel;
    }

    public List<Long> getDepartmentIds() {
        return departmentIds;
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
