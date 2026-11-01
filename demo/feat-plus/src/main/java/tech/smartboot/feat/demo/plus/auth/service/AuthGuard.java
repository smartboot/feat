package tech.smartboot.feat.demo.plus.auth.service;

import tech.smartboot.feat.demo.plus.auth.token.TokenSession;
import tech.smartboot.feat.demo.plus.auth.token.TokenStore;
import tech.smartboot.feat.demo.plus.common.DomainException;
import tech.smartboot.feat.router.Context;

/**
 * 从请求里解析 Bearer 令牌。
 * <p>
 * Feat 的方法拦截器拿不到 HTTP Context，因此鉴权放在 Controller 方法内部完成，
 * 需要登录的方法统一注入 Context 后调用本类。
 */
public final class AuthGuard {

    private static final String BEARER_PREFIX = "Bearer ";

    private AuthGuard() {
    }

    public static String readToken(Context context) {
        if (context == null) {
            return null;
        }
        String header = context.Request.getHeader("Authorization");
        if (header == null) {
            return null;
        }
        String token = header.trim();
        if (token.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            token = token.substring(BEARER_PREFIX.length()).trim();
        }
        return token.isEmpty() ? null : token;
    }

    public static TokenSession resolve(Context context, TokenStore tokenStore) {
        return tokenStore.find(readToken(context));
    }

    /**
     * 要求已登录，返回当前用户 ID；未登录直接抛 401。
     */
    public static long require(Context context, TokenStore tokenStore) {
        TokenSession session = resolve(context, tokenStore);
        if (session == null) {
            throw DomainException.unauthorized();
        }
        return session.getUserId();
    }
}
