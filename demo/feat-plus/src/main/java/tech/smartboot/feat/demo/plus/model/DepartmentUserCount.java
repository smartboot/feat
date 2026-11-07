package tech.smartboot.feat.demo.plus.model;

/**
 * 部门成员数聚合结果。单独成类，不能写成 Mapper 的内部类（AOT 处理器会解析失败）。
 */
public class DepartmentUserCount {

    private Long departmentId;
    private long total;

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }
}
