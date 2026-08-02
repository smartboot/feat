package tech.smartboot.feat.demo.plus.auth.token;

public class TokenSession {
    private final long userId;
    private final long expiresAtEpochMillis;

    public TokenSession(long userId, long expiresAtEpochMillis) {
        this.userId = userId;
        this.expiresAtEpochMillis = expiresAtEpochMillis;
    }

    public long getUserId() {
        return userId;
    }

    public long getExpiresAtEpochMillis() {
        return expiresAtEpochMillis;
    }
}
