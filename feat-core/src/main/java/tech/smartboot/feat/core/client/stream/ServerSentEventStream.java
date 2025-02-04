package tech.smartboot.feat.core.client.stream;

import tech.smartboot.feat.core.client.HttpResponse;

import java.io.IOException;

public class ServerSentEventStream implements Stream {
    public static final String DATA = "data";
    public static final String TYPE = "type";
    public static final String ID = "id";

    @Override
    public void stream(HttpResponse response, byte[] bytes, boolean end) throws IOException {

    }

}
