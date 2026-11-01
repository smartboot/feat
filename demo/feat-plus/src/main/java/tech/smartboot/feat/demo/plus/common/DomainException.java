package tech.smartboot.feat.demo.plus.common;

/**
 * 业务校验失败。由 Controller 捕获并转成 RestResult.fail，保留给前端的可读提示。
 */
public class DomainException extends RuntimeException {

    private final boolean unauthorized;
    private final boolean notFound;

    public DomainException(String message) {
        this(message, false, false);
    }

    public DomainException(String message, boolean unauthorized, boolean notFound) {
        super(message);
        this.unauthorized = unauthorized;
        this.notFound = notFound;
    }

    public static DomainException unauthorized() {
        return new DomainException("未登录或登录已过期", true, false);
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
}
