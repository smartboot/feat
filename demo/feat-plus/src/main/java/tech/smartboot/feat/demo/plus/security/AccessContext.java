package tech.smartboot.feat.demo.plus.security;

import java.util.Collections;
import java.util.List;

/**
 * 一次请求的访问上下文：谁、能做什么、能对谁做。
 * <p>
 * 由 AccessGuard 解析，Controller 通过它拿操作人 ID，Service 通过它做行级过滤。
 * <p>
 * 两个维度是正交的，不要混为一谈：
 * <ul>
 *   <li>{@link #can(String)} —— 权限点，来自角色的 permissions 并集；</li>
 *   <li>{@link #visibleDepartmentIds()} / {@link #canReach} —— 数据范围，来自角色的 department_scope。</li>
 * </ul>
 */
public final class AccessContext {

    private final long userId;
    private final Long departmentId;
    private final List<String> permissions;
    /** 数据范围原值（已归一化），仅用于前端展示与日志。 */
    private final int dataScope;
    /** true 表示不做数据过滤。 */
    private final boolean unrestricted;
    /** true 表示只能看到自己。 */
    private final boolean selfOnly;
    /** 可见部门集合；null 表示不按部门限制。空集合表示什么都看不到。 */
    private final List<Long> departmentIds;

    private AccessContext(long userId, Long departmentId, List<String> permissions, int dataScope,
                          boolean unrestricted, boolean selfOnly, List<Long> departmentIds) {
        this.userId = userId;
        this.departmentId = departmentId;
        this.permissions = permissions;
        this.dataScope = dataScope;
        this.unrestricted = unrestricted;
        this.selfOnly = selfOnly;
        this.departmentIds = departmentIds;
    }

    public static AccessContext unrestricted(long userId, Long departmentId, List<String> permissions) {
        return new AccessContext(userId, departmentId, permissions, DataScope.ALL, true, false, null);
    }

    public static AccessContext scoped(long userId, Long departmentId, List<String> permissions,
                                       int dataScope, boolean selfOnly, List<Long> departmentIds) {
        return new AccessContext(userId, departmentId, permissions, dataScope, false, selfOnly, departmentIds);
    }

    public long getUserId() {
        return userId;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public int getDataScope() {
        return dataScope;
    }

    public boolean isUnrestricted() {
        return unrestricted;
    }

    public boolean isSelfOnly() {
        return selfOnly;
    }

    public boolean can(String permission) {
        return Permission.contains(permissions, permission);
    }

    /**
     * 查询用的部门过滤集合：null 表示不加条件；空集合表示结果必然为空。
     * 调用方拿到空集合时应传一个不可能的 id（如 -1）给 SQL，避免拼出 IN ()。
     */
    public List<Long> visibleDepartmentIds() {
        return departmentIds;
    }

    /**
     * 直接可以塞进 MyBatis foreach 的部门过滤集合。
     * <p>
     * 把「空集合替换成 -1」这一步收在这里，是因为每个走数据权限的查询都需要它：
     * MyBatis 的 foreach 遇到空集合会拼出非法的 IN ()。
     */
    public List<Long> queryDepartmentIds() {
        if (departmentIds == null) {
            return null;
        }
        return departmentIds.isEmpty() ? Collections.singletonList(-1L) : departmentIds;
    }

    /**
     * 非 null 表示 SQL 要追加「只能是自己这一行」的条件。
     */
    public Long selfOnlyUserId() {
        return selfOnly ? Long.valueOf(userId) : null;
    }

    /**
     * 单条数据是否在范围内。列表过滤只挡住「翻找」，挡不住「直接猜 id」，
     * 所以每个按 id 的读写都要过这一关。
     * <p>
     * 自己永远可见：否则「仅本人」范围的账号点开自己的详情会被拒。
     */
    public boolean canReach(long targetUserId, Long targetDepartmentId) {
        if (unrestricted) {
            return true;
        }
        if (targetUserId == userId) {
            return true;
        }
        if (selfOnly || departmentIds == null) {
            return false;
        }
        return targetDepartmentId != null && departmentIds.contains(targetDepartmentId);
    }

    /**
     * 能否把账号放进某个部门（新建账号、或把人调到别的部门时用）。
     * <p>
     * 「仅本人」范围下不允许放置任何账号：否则可以把自己创建到任意部门，
     * 再回头修改自己去绕过范围限制。
     */
    public boolean canPlaceIn(Long targetDepartmentId) {
        if (unrestricted) {
            return true;
        }
        if (selfOnly || departmentIds == null) {
            return false;
        }
        return targetDepartmentId != null && departmentIds.contains(targetDepartmentId);
    }

    /**
     * 给前端的说明文案，例如「数据范围：本部门及以下」。
     */
    public String describeScope() {
        if (unrestricted) {
            return "全部数据";
        }
        if (selfOnly) {
            return "仅本人";
        }
        if (departmentIds == null || departmentIds.isEmpty()) {
            return "未配置可见部门";
        }
        return DataScope.label(dataScope) + "（" + departmentIds.size() + " 个部门）";
    }

    public static AccessContext none() {
        return new AccessContext(0L, null, Collections.<String>emptyList(), DataScope.SELF, false, true,
                Collections.<Long>emptyList());
    }
}
