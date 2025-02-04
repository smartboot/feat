package tech.smartboot.feat.core.client.stream;

import tech.smartboot.feat.core.client.HttpResponse;
import tech.smartboot.feat.core.client.impl.HttpResponseImpl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class StringStream implements Stream {
    private final ByteArrayOutputStream bodyStream = new ByteArrayOutputStream();
    private final HttpResponseImpl response;

    public StringStream(HttpResponseImpl response) {
        this.response = response;
    }

    @Override
    public void stream(HttpResponse r, byte[] bytes, boolean end) throws IOException {
        try {
            bodyStream.write(bytes);
            if (end) {
                response.setBody(bodyStream.toString());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
