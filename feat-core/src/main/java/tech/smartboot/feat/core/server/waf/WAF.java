package tech.smartboot.feat.core.server.waf;

import tech.smartboot.feat.core.common.enums.HttpStatus;
import tech.smartboot.feat.core.common.utils.CollectionUtils;
import tech.smartboot.feat.core.server.ServerOptions;
import tech.smartboot.feat.core.server.impl.HttpEndpoint;

public class WAF {
    public static void methodCheck(ServerOptions configuration, HttpEndpoint request) {
        WafOptions wafOptions = configuration.getWafConfiguration();
        if (!wafOptions.isEnable()) {
            return;
        }
        if (!wafOptions.getAllowMethods().isEmpty() && !wafOptions.getAllowMethods().contains(request.getMethod())) {
            throw new WafException(HttpStatus.METHOD_NOT_ALLOWED, WafOptions.DESC);
        }
        if (!wafOptions.getDenyMethods().isEmpty() && wafOptions.getDenyMethods().contains(request.getMethod())) {
            throw new WafException(HttpStatus.METHOD_NOT_ALLOWED, WafOptions.DESC);
        }
    }

    public static void checkUri(ServerOptions configuration, HttpEndpoint request) {
        WafOptions wafOptions = configuration.getWafConfiguration();
        if (!wafOptions.isEnable()) {
            return;
        }
        if (request.getUri().equals("/") || CollectionUtils.isEmpty(wafOptions.getAllowUriPrefixes()) && CollectionUtils.isEmpty(wafOptions.getAllowUriSuffixes())) {
            return;
        }
        for (String prefix : wafOptions.getAllowUriPrefixes()) {
            if (request.getUri().startsWith(prefix)) {
                return;
            }
        }
        for (String suffix : wafOptions.getAllowUriSuffixes()) {
            if (request.getUri().endsWith(suffix)) {
                return;
            }
        }
        throw new WafException(HttpStatus.BAD_REQUEST, WafOptions.DESC);
    }
}
