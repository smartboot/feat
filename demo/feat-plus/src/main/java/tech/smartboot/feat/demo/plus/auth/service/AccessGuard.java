package tech.smartboot.feat.demo.plus.auth.service;

import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Bean;
import tech.smartboot.feat.demo.plus.auth.mapper.AuthMapper;
import tech.smartboot.feat.demo.plus.auth.model.AuthAccount;
import tech.smartboot.feat.demo.plus.auth.token.TokenStore;
import tech.smartboot.feat.demo.plus.common.DomainException;
import tech.smartboot.feat.router.Context;

/**
 * 权限校验入口。
 * <p>
 * 每个需要鉴权的 Controller 注入本类，用 {@link #require(Context, String)} 一次性拿到
 * 操作人 ID、权限集合与数据范围：未登录抛 401，权限不足抛 403，两者由 ApiController 翻译成状态码。
 * <p>
 * 权限每次请求都从库里读，所以改了角色权限立即生效，无需重新登录。
 * 多角色的合并规则见 {@link DataScopeResolver}。
 */
@Bean
public class AccessGuard {

    @Autowired
    private TokenStore tokenStore;

    @Autowired
    private AuthMapper authMapper;

    @Autowired
    private DataScopeResolver dataScopeResolver;

    /**
     * 只要求登录，不要求具体权限（如读取自己的通知）。
     */
    public AccessContext requireLogin(Context context) {
        long userId = AuthGuard.require(context, tokenStore);
        return resolve(userId);
    }

    /**
     * 要求登录且具备指定权限点。
     */
    public AccessContext require(Context context, String permission) {
        AccessContext access = requireLogin(context);
        if (!access.can(permission)) {
            throw DomainException.forbidden("当前角色没有该操作的权限：" + permission);
        }
        return access;
    }

    /**
     * 按用户 id 解析上下文，供没有 HTTP 上下文的场景复用（如登录响应）。
     */
    public AccessContext resolve(long userId) {
        AuthAccount account = authMapper.selectById(userId);
        if (account == null) {
            throw DomainException.unauthorized();
        }
        account.setRoles(authMapper.selectRolesByUserId(userId));
        return dataScopeResolver.resolve(account);
    }
}
