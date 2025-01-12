package tech.smartboot.feat.core;

import tech.smartboot.feat.core.server.FeatOptions;
import tech.smartboot.feat.core.server.HttpServer;

public class Feat {
    public static HttpServer createHttpServer() {
        return createHttpServer(new FeatOptions());
    }

    public static HttpServer createHttpServer(FeatOptions options) {
        return new HttpServer(options);
    }
}
