package tech.smartboot.feat.demo.plus.common;

/**
 * 机器可读的错误分类。HTTP 状态码只有五档，第三方集成时「用户名重复」和「参数缺字段」
 * 在 400 里没法区分，排查时也只能靠人读 message——错误码就是给程序看的那个稳定锚点。
 * <p>
 * 刻意不把错误码做成一大张逐条编号的表（比如 USER_1001、USER_1002）：编号表最大的问题
 * 是「加一条要记得登记、删一条没人敢清」，demo 规模下五档分类 + 可读 message 已经够用；
 * 真要做逐条编号，等出现第一个需要按错误码分支的调用方再扩展，枚举加值是兼容变更。
 */
public enum ErrorCode {
    BAD_REQUEST(400, "Bad Request"),
    UNAUTHORIZED(401, "Unauthorized"),
    FORBIDDEN(403, "Forbidden"),
    NOT_FOUND(404, "Not Found"),
    INTERNAL_ERROR(500, "Internal Server Error");

    private final int httpStatus;
    private final String reason;

    ErrorCode(int httpStatus, String reason) {
        this.httpStatus = httpStatus;
        this.reason = reason;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public String getReason() {
        return reason;
    }
}
