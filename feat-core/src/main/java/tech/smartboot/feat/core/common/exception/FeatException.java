package tech.smartboot.feat.core.common.exception;

public class FeatException extends RuntimeException {
    public FeatException(String message) {
        super(message);
    }

    public FeatException(Throwable cause) {
        super(cause);
    }
}
