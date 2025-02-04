package tech.smartboot.feat.core.client;

import java.io.IOException;

public interface BodyStreaming {
    void stream(HttpResponse response, byte[] bytes, boolean end) throws IOException;

}
