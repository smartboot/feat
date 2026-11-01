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
 * 操作人 ID 与权限集合：未登录抛 401，权限不足抛 403，两者由 ApiController 翻译成状态码。
 * <p>
 * 权限每次请求都从库里读，所以改了角色权限立即生效，无需重新登录。
 */
@Bean
public class AccessGuard {

    @Autowired
    private TokenStore tokenStore;

    @Autowired
    private AuthMapper authMapper;

    /**
     * 只要求登录，不要求具体权限（如读取自己的通知）。
     */
    public AccessContext requireLogin(Context context) {
        long userId = AuthGuard.require(context, tokenStore);
        AuthAccount account = authMapper.selectById(userId);
        if (account == null) {
            throw DomainException.unauthorized();
        }
        return new AccessContext(userId, account.effectivePermissions());
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
}
