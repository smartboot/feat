package tech.smartboot.feat.demo.plus.auth.token;

public class IssuedToken {
    private final String value;
    private final long expiresIn;

    public IssuedToken(String value, long expiresIn) {
        this.value = value;
        this.expiresIn = expiresIn;
    }

    public String getValue() {
        return value;
    }

    public long getExpiresIn() {
        return expiresIn;
    }
}
