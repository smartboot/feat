package tech.smartboot.feat.core;

import tech.smartboot.feat.core.server.FeatServerOptions;
import tech.smartboot.feat.core.server.HttpServer;

import java.util.function.Consumer;

public class Feat {
    public static HttpServer createHttpServer() {
        return createHttpServer(new FeatServerOptions());
    }

    public static HttpServer createHttpServer(FeatServerOptions options) {
        return new HttpServer(options);
    }

    public static HttpServer createHttpServer(Consumer<FeatServerOptions> options) {
        FeatServerOptions opt = new FeatServerOptions();
        options.accept(opt);
        return createHttpServer(opt);
    }
}
