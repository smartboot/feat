package tech.smartboot.feat.demo.plus.auth.service;

import java.util.List;

import tech.smartboot.feat.demo.plus.common.Permission;

/**
 * 一次请求的访问上下文：当前用户 + 其角色生效的权限点。
 * <p>
 * 由 AccessGuard 解析，Controller 拿它取 operatorId，也用它判断能否执行敏感动作。
 */
public final class AccessContext {

    private final long userId;
    private final List<String> permissions;

    public AccessContext(long userId, List<String> permissions) {
        this.userId = userId;
        this.permissions = permissions;
    }

    public long getUserId() {
        return userId;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public boolean can(String permission) {
        return Permission.contains(permissions, permission);
    }
}
