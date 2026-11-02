package tech.smartboot.feat.demo.plus.common;

/**
 * 业务校验失败。由 Controller 捕获并转成 RestResult.fail，保留给前端的可读提示。
 * <p>
 * 带一个 {@link ErrorCode}：HTTP 响应体里的 code 与状态码对齐就靠它，
 * 三种布尔标记（unauthorized / notFound / forbidden）是历史遗留的判定方式，
 * 已由 ErrorCode 覆盖，保留只为兼容老调用方。
 */
public class DomainException extends RuntimeException {

    private final ErrorCode errorCode;
    private final boolean unauthorized;
    private final boolean notFound;
    private final boolean forbidden;

    public DomainException(String message) {
        this(message, ErrorCode.BAD_REQUEST);
    }

    public DomainException(String message, boolean unauthorized, boolean notFound) {
        this(message, resolve(unauthorized, notFound));
    }

    public DomainException(String message, boolean unauthorized, boolean notFound, boolean forbidden) {
        this(message, resolve(unauthorized, notFound, forbidden));
    }

    private DomainException(String message, ErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.unauthorized = errorCode == ErrorCode.UNAUTHORIZED;
        this.notFound = errorCode == ErrorCode.NOT_FOUND;
        this.forbidden = errorCode == ErrorCode.FORBIDDEN;
    }

    private static ErrorCode resolve(boolean unauthorized, boolean notFound) {
        if (unauthorized) {
            return ErrorCode.UNAUTHORIZED;
        }
        return notFound ? ErrorCode.NOT_FOUND : ErrorCode.BAD_REQUEST;
    }

    private static ErrorCode resolve(boolean unauthorized, boolean notFound, boolean forbidden) {
        if (forbidden) {
            return ErrorCode.FORBIDDEN;
        }
        return resolve(unauthorized, notFound);
    }

    public static DomainException unauthorized() {
        return new DomainException("未登录或登录已过期", ErrorCode.UNAUTHORIZED);
    }

    public static DomainException forbidden(String message) {
        return new DomainException(message, ErrorCode.FORBIDDEN);
    }

    public static DomainException notFound(String message) {
        return new DomainException(message, ErrorCode.NOT_FOUND);
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public boolean isUnauthorized() {
        return unauthorized;
    }

    public boolean isNotFound() {
        return notFound;
    }

    public boolean isForbidden() {
        return forbidden;
    }
}
