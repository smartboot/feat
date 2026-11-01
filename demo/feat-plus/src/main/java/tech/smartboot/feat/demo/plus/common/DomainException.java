package tech.smartboot.feat.demo.plus.common;

/**
 * 业务校验失败。由 Controller 捕获并转成 RestResult.fail，保留给前端的可读提示。
 */
public class DomainException extends RuntimeException {

    private final boolean unauthorized;
    private final boolean notFound;
    private final boolean forbidden;

    public DomainException(String message) {
        this(message, false, false, false);
    }

    public DomainException(String message, boolean unauthorized, boolean notFound) {
        this(message, unauthorized, notFound, false);
    }

    public DomainException(String message, boolean unauthorized, boolean notFound, boolean forbidden) {
        super(message);
        this.unauthorized = unauthorized;
        this.notFound = notFound;
        this.forbidden = forbidden;
    }

    public static DomainException unauthorized() {
        return new DomainException("未登录或登录已过期", true, false);
    }

    public static DomainException forbidden(String message) {
        return new DomainException(message, false, false, true);
    }

    public static DomainException notFound(String message) {
        return new DomainException(message, false, true);
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
