package tech.smartboot.feat.core;

import tech.smartboot.feat.core.server.HttpBootstrap;

public class Feat {
    public static HttpBootstrap createHttpServer() {
        return new HttpBootstrap();
    }
}
