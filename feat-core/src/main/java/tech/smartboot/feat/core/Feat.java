package tech.smartboot.feat.core;

import tech.smartboot.feat.core.server.HttpServer;
import tech.smartboot.feat.core.server.ServerOptions;

import java.util.function.Consumer;

public class Feat {
    public static HttpServer createHttpServer() {
        return createHttpServer(new ServerOptions());
    }

    public static HttpServer createHttpServer(ServerOptions options) {
        return new HttpServer(options);
    }

    public static HttpServer createHttpServer(Consumer<ServerOptions> options) {
        ServerOptions opt = new ServerOptions();
        options.accept(opt);
        return createHttpServer(opt);
    }

}
