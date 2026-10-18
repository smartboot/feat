package tech.smartboot.feat.gateway.policy;

import tech.smartboot.feat.gateway.config.GatewayConfig;
import tech.smartboot.feat.gateway.config.KeyHash;
import tech.smartboot.feat.gateway.config.KeyPolicy;

/**
 * 本地鉴权：Bearer token -> KeyPolicy。
 * <p>
 * 只做内存哈希比对，不访问任何远程服务——断网、控制面宕机都不影响鉴权。
 */
public final class AuthPolicy {
    private AuthPolicy() {
    }

    public static KeyPolicy authenticate(GatewayConfig config, String authorization) {
        String token = extractToken(authorization);
        if (token == null || token.isEmpty()) {
            return null;
        }
        for (KeyPolicy key : config.keys()) {
            if (KeyHash.matches(token, key.keyHash())) {
                return key;
            }
        }
        return null;
    }

    public static String extractToken(String authorization) {
        if (authorization == null) {
            return null;
        }
        String value = authorization.trim();
        if (value.length() > 7 && value.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return value.substring(7).trim();
        }
        // 少数客户端直接塞裸 token，兼容一下，但要求非空且不含空格
        if (value.indexOf(' ') < 0 && value.length() >= 8) {
            return value;
        }
        return null;
    }
}
