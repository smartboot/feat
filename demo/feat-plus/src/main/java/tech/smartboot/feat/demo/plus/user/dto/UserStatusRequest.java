package tech.smartboot.feat.demo.plus.user.dto;

/**
 * 启停用户的请求体。
 */
public class UserStatusRequest {
    private Integer status;

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
