package tech.smartboot.feat.demo.plus.auth.dto;

/**
 * 改自己的密码。
 * <p>
 * 必须同时提供旧密码：只凭一个有效令牌就能改密码，等于令牌一旦泄露就能永久接管账号。
 */
public class ChangePasswordRequest {
    private String oldPassword;
    private String newPassword;

    public String getOldPassword() {
        return oldPassword;
    }

    public void setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
