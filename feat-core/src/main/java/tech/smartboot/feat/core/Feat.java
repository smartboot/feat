package tech.smartboot.feat.core;

import tech.smartboot.feat.core.server.FeatOptions;
import tech.smartboot.feat.core.server.HttpServer;

import java.util.function.Consumer;

public class Feat {
    public static HttpServer createHttpServer() {
        return createHttpServer(new FeatOptions());
    }

    public static HttpServer createHttpServer(FeatOptions options) {
        return new HttpServer(options);
    }

    public static HttpServer createHttpServer(Consumer<FeatOptions> options) {
        FeatOptions opt = new FeatOptions();
        options.accept(opt);
        return createHttpServer(opt);
    }
}
