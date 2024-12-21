package tech.smartboot.feat.core.server.waf;

import tech.smartboot.feat.core.common.enums.HttpStatus;
import tech.smartboot.feat.core.common.exception.HttpException;

public class WafException extends HttpException {
    public WafException(HttpStatus httpStatus) {
        super(httpStatus.value(), WafConfiguration.DESC);
    }

    public WafException(HttpStatus httpStatus, String desc) {
        super(httpStatus.value(), desc);
    }
}
