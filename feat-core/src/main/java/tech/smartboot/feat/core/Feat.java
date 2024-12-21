package tech.smartboot.feat.core;

import tech.smartboot.feat.core.server.HttpServer;

public class Feat {
    public static HttpServer createHttpServer() {
        return new HttpServer();
    }
}
