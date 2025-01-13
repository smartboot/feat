package tech.smartboot.feat.core.server.waf;

import tech.smartboot.feat.core.common.enums.HttpStatus;
import tech.smartboot.feat.core.common.utils.CollectionUtils;
import tech.smartboot.feat.core.server.FeatServerOptions;
import tech.smartboot.feat.core.server.impl.Request;

public class WAF {
    public static void methodCheck(FeatServerOptions configuration, Request request) {
        WafConfiguration wafConfiguration = configuration.getWafConfiguration();
        if (!wafConfiguration.isEnable()) {
            return;
        }
        if (!wafConfiguration.getAllowMethods().isEmpty() && !wafConfiguration.getAllowMethods().contains(request.getMethod())) {
            throw new WafException(HttpStatus.METHOD_NOT_ALLOWED, WafConfiguration.DESC);
        }
        if (!wafConfiguration.getDenyMethods().isEmpty() && wafConfiguration.getDenyMethods().contains(request.getMethod())) {
            throw new WafException(HttpStatus.METHOD_NOT_ALLOWED, WafConfiguration.DESC);
        }
    }

    public static void checkUri(FeatServerOptions configuration, Request request) {
        WafConfiguration wafConfiguration = configuration.getWafConfiguration();
        if (!wafConfiguration.isEnable()) {
            return;
        }
        if (request.getUri().equals("/") || CollectionUtils.isEmpty(wafConfiguration.getAllowUriPrefixes()) && CollectionUtils.isEmpty(wafConfiguration.getAllowUriSuffixes())) {
            return;
        }
        for (String prefix : wafConfiguration.getAllowUriPrefixes()) {
            if (request.getUri().startsWith(prefix)) {
                return;
            }
        }
        for (String suffix : wafConfiguration.getAllowUriSuffixes()) {
            if (request.getUri().endsWith(suffix)) {
                return;
            }
        }
        throw new WafException(HttpStatus.BAD_REQUEST, WafConfiguration.DESC);
    }
}
