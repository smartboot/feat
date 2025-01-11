package tech.smartboot.feat.core.server.impl;

import java.io.IOException;
import java.nio.ByteBuffer;

public abstract class HttpUpgradeHandler {
    protected Request request;

    void setRequest(Request request) {
        this.request = request;
    }

    public abstract void init() throws IOException;

    public abstract void onBodyStream(ByteBuffer buffer);

    /**
     * 在客户端关闭连接时调用
     */
    public void destroy() {
    }
}
