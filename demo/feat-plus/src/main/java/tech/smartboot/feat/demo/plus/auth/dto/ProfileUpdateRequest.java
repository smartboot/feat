package tech.smartboot.feat.demo.plus.auth.dto;

/**
 * 改自己的昵称与邮箱。
 * <p>
 * 没有 username 字段：用户名是登录标识，不允许自助修改。
 * 如果以后要支持改用户名，应该是独立接口 + 二次验证，而不是混在资料编辑里。
 */
public class ProfileUpdateRequest {
    private String displayName;
    private String email;

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
