package tech.smartboot.feat.demo.plus.auth.dto;

public class LoginResponse {
    private String accessToken;
    private String tokenType;
    private long expiresIn;
    private CurrentUserResponse user;

    public LoginResponse() {
    }

    public LoginResponse(String accessToken, long expiresIn, CurrentUserResponse user) {
        this.accessToken = accessToken;
        this.tokenType = "Bearer";
        this.expiresIn = expiresIn;
        this.user = user;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public CurrentUserResponse getUser() {
        return user;
    }

    public void setUser(CurrentUserResponse user) {
        this.user = user;
    }
}
