package tech.smartboot.feat.core.server.waf;

import tech.smartboot.feat.core.common.enums.HttpStatus;
import tech.smartboot.feat.core.common.utils.CollectionUtils;
import tech.smartboot.feat.core.server.HttpServerConfiguration;
import tech.smartboot.feat.core.server.decode.Decoder;
import tech.smartboot.feat.core.server.impl.Request;

import java.nio.ByteBuffer;

public class UriWafDecoder extends AbstractWafDecoder {


    public UriWafDecoder(HttpServerConfiguration configuration) {
        super(configuration);
    }

    @Override
    protected Decoder decode0(ByteBuffer byteBuffer, Request request) {
        WafConfiguration wafConfiguration = getConfiguration().getWafConfiguration();
        if (request.getUri().equals("/") || CollectionUtils.isEmpty(wafConfiguration.getAllowUriPrefixes()) && CollectionUtils.isEmpty(wafConfiguration.getAllowUriSuffixes())) {
            return null;
        }
        for (String prefix : wafConfiguration.getAllowUriPrefixes()) {
            if (request.getUri().startsWith(prefix)) {
                return null;
            }
        }
        for (String suffix : wafConfiguration.getAllowUriSuffixes()) {
            if (request.getUri().endsWith(suffix)) {
                return null;
            }
        }
        throw new WafException(HttpStatus.BAD_REQUEST, WafConfiguration.DESC);
    }
}
