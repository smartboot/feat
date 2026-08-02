package tech.smartboot.feat.demo.plus.auth.token;

public interface TokenStore {
    IssuedToken issue(long userId);

    TokenSession find(String token);

    void remove(String token);
}
