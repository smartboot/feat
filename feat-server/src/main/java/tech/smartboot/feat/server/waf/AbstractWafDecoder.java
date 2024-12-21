package tech.smartboot.feat.server.waf;

import tech.smartboot.feat.server.HttpServerConfiguration;
import tech.smartboot.feat.server.decode.AbstractDecoder;

public abstract class AbstractWafDecoder extends AbstractDecoder {

    public AbstractWafDecoder(HttpServerConfiguration configuration) {
        super(configuration);
    }
}
