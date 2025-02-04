package tech.smartboot.feat.core.client.stream;

import tech.smartboot.feat.core.client.HttpResponse;

import java.io.IOException;

public interface BodyStreaming {
    BodyStreaming SKIP_BODY_STREAMING = (response, bytes, end) -> {
    };

    void stream(HttpResponse response, byte[] bytes, boolean end) throws IOException;

}
