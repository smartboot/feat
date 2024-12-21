package tech.smartboot.feat.server.waf;

import tech.smartboot.feat.common.enums.HttpStatus;
import tech.smartboot.feat.common.exception.HttpException;

public class WafException extends HttpException {
    public WafException(HttpStatus httpStatus) {
        super(httpStatus.value(), WafConfiguration.DESC);
    }

    public WafException(HttpStatus httpStatus, String desc) {
        super(httpStatus.value(), desc);
    }
}
