package tech.smartboot.feat.demo.plus.user.dto;

/**
 * 重置密码的响应体，字段名与前端约定一致。
 */
public class TemporaryPasswordResponse {
    private final String temporaryPassword;

    public TemporaryPasswordResponse(String temporaryPassword) {
        this.temporaryPassword = temporaryPassword;
    }

    public String getTemporaryPassword() {
        return temporaryPassword;
    }
}
