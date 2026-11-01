package tech.smartboot.feat.demo.plus.common;

import java.util.Arrays;
import java.util.List;

/**
 * 角色数据范围字典，对应 sys_role.department_scope。
 * <p>
 * 编码沿用后台系统里最通行的一套取值（1 全部 / 2 自定义 / 3 本部门 / 4 本部门及以下 / 5 仅本人），
 * 熟悉同类系统的开发者看数字就知道含义。
 * <p>
 * 注意：权限点（Permission）决定「能不能做」，数据范围（这里）决定「能对谁做」。
 * 两者是两个正交维度，不要混在一条字符串里。
 */
public final class DataScope {

    /** 不做任何过滤，全部数据可见。 */
    public static final int ALL = 1;

    /** 只看 sys_role_dept 里配置的部门。 */
    public static final int CUSTOM = 2;

    /** 只看自己所在部门。 */
    public static final int DEPARTMENT = 3;

    /** 自己所在部门及其全部下级部门。 */
    public static final int DEPARTMENT_AND_CHILD = 4;

    /** 只看自己。 */
    public static final int SELF = 5;

    public static final List<Integer> ALL_VALUES = Arrays.asList(ALL, CUSTOM, DEPARTMENT, DEPARTMENT_AND_CHILD, SELF);

    private DataScope() {
    }

    /**
     * 把外部传入的值规整成合法档位，非法值一律当成最保守的「仅本人」。
     * 数据范围是安全边界，遇到看不懂的输入时应该收紧而不是放开。
     */
    public static int normalize(Integer raw) {
        if (raw == null || !ALL_VALUES.contains(raw)) {
            return SELF;
        }
        return raw;
    }

    public static String label(int scope) {
        switch (scope) {
            case ALL:
                return "全部数据";
            case CUSTOM:
                return "自定义部门";
            case DEPARTMENT:
                return "本部门";
            case DEPARTMENT_AND_CHILD:
                return "本部门及以下";
            case SELF:
                return "仅本人";
            default:
                return "未知范围";
        }
    }
}
