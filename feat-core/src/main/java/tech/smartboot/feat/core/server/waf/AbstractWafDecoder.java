package tech.smartboot.feat.core.server.waf;

import tech.smartboot.feat.core.server.HttpServerConfiguration;
import tech.smartboot.feat.core.server.decode.AbstractDecoder;

public abstract class AbstractWafDecoder extends AbstractDecoder {

    public AbstractWafDecoder(HttpServerConfiguration configuration) {
        super(configuration);
    }
}
