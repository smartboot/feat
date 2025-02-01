package tech.smartboot.feat.core.client;

public interface BodySteaming {
    void stream(HttpResponse response, byte[] bytes);
}
