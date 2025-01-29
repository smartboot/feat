package tech.smartboot.feat.core.server.impl;

import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.HttpResponse;

import java.io.IOException;
import java.nio.ByteBuffer;

public abstract class Upgrade {
    protected HttpEndpoint request;

    void setRequest(HttpEndpoint request) {
        this.request = request;
    }

    public abstract void init(HttpRequest request, HttpResponse response) throws IOException;

    public abstract void onBodyStream(ByteBuffer buffer);

    /**
     * 在客户端关闭连接时调用
     */
    public void destroy() {
    }
}
