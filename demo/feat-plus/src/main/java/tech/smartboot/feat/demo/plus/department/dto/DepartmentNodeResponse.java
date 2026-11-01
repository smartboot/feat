package tech.smartboot.feat.demo.plus.department.dto;

import java.util.ArrayList;
import java.util.List;

import tech.smartboot.feat.demo.plus.common.DateFormats;
import tech.smartboot.feat.demo.plus.department.model.DepartmentRecord;

/**
 * 部门树节点，字段与前端 DepartmentNode 对齐。
 * children 为空数组而不是 null，前端可以无条件展开渲染。
 */
public class DepartmentNodeResponse {
    private long id;
    private long parentId;
    private String code;
    private String name;
    private int sortOrder;
    private String description;
    private int status;
    /** 直属成员数，不含子部门。 */
    private long userCount;
    private String createdTime;
    private String editTime;
    private List<DepartmentNodeResponse> children = new ArrayList<>();

    public static DepartmentNodeResponse from(DepartmentRecord record, long userCount) {
        DepartmentNodeResponse node = new DepartmentNodeResponse();
        node.id = record.getId();
        node.parentId = record.getParentId();
        node.code = record.getCode();
        node.name = record.getName();
        node.sortOrder = record.getSortOrder();
        node.description = record.getDescription();
        node.status = record.getStatus();
        node.userCount = userCount;
        node.createdTime = DateFormats.format(record.getCreatedTime());
        node.editTime = DateFormats.format(record.getEditTime());
        return node;
    }

    public long getId() {
        return id;
    }

    public long getParentId() {
        return parentId;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public String getDescription() {
        return description;
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

    public List<DepartmentNodeResponse> getChildren() {
        return children;
    }
}
