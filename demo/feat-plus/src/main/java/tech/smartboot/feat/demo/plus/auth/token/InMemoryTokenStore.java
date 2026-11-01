package tech.smartboot.feat.demo.plus.auth.token;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import tech.smartboot.feat.cloud.annotation.Bean;
import tech.smartboot.feat.cloud.annotation.Value;

/**
 * 进程内令牌存储。
 * <p>
 * 演示环境不引入 Redis：服务重启后令牌失效，重新登录即可。
 * 有效期取 feat.yaml 的 feat-plus.auth.token-ttl-seconds，缺省 7200 秒。
 * <p>
 * 必须显式命名为 tokenStore：Feat 的 @Autowired 按字段名匹配 Bean，
 * 而实现类默认注册名是 inMemoryTokenStore，与各 Controller 里的字段名对不上。
 */
@Bean("tokenStore")
public class InMemoryTokenStore implements TokenStore {

    private static final long DEFAULT_TTL_SECONDS = 7200L;

    private final Map<String, TokenSession> sessions = new ConcurrentHashMap<>();

    @Value("${feat-plus.auth.token-ttl-seconds}")
    private String ttlSecondsText;

    /**
     * Feat 的 @Value 通过 setter 注入，字段注入会在编译期报
     * "no setter method for field"，因此这个方法不能删。
     */
    public void setTtlSecondsText(String ttlSecondsText) {
        this.ttlSecondsText = ttlSecondsText;
    }

    @Override
    public IssuedToken issue(long userId) {
        long ttl = ttlSeconds();
        String token = UUID.randomUUID().toString().replace("-", "");
        sessions.put(token, new TokenSession(userId, System.currentTimeMillis() + ttl * 1000L));
        return new IssuedToken(token, ttl);
    }

    @Override
    public TokenSession find(String token) {
        if (token == null) {
            return null;
        }
        TokenSession session = sessions.get(token);
        if (session == null) {
            return null;
        }
        if (session.getExpiresAtEpochMillis() <= System.currentTimeMillis()) {
            sessions.remove(token);
            return null;
        }
        return session;
    }

    @Override
    public void remove(String token) {
        if (token != null) {
            sessions.remove(token);
        }
    }

    @Override
    public int removeOthers(long userId, String keepToken) {
        int removed = 0;
        for (Map.Entry<String, TokenSession> entry : sessions.entrySet()) {
            if (entry.getValue().getUserId() != userId) {
                continue;
            }
            if (entry.getKey().equals(keepToken)) {
                continue;
            }
            // 先判 null 再用 equals：keepToken 为空时不能反向调用 keepToken.equals(...)
            if (sessions.remove(entry.getKey()) != null) {
                removed++;
            }
        }
        return removed;
    }

    private long ttlSeconds() {
        if (ttlSecondsText == null || ttlSecondsText.trim().isEmpty()) {
            return DEFAULT_TTL_SECONDS;
        }
        try {
            long value = Long.parseLong(ttlSecondsText.trim());
            return value > 0 ? value : DEFAULT_TTL_SECONDS;
        } catch (NumberFormatException e) {
            return DEFAULT_TTL_SECONDS;
        }
    }
}
