package tech.smartboot.feat.core.client;

import java.io.IOException;

public interface BodySteaming {
    void stream(HttpResponse response, byte[] bytes) throws IOException;

    default void end(HttpResponse response) throws IOException {
    }

}
