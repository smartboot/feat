package tech.smartboot.feat.core;

import tech.smartboot.feat.core.apt.ApplicationContext;
import tech.smartboot.feat.core.server.HttpServer;
import tech.smartboot.feat.core.server.ServerOptions;
import tech.smartboot.feat.core.server.handler.Router;

import java.util.Collection;
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

    public static HttpServer createServer(Consumer<ServerOptions> options, Collection<String> packages) {
        HttpServer server = createHttpServer(options);
        ApplicationContext application = new ApplicationContext(packages);
        Router router = new Router();
        application.start(router);
        server.httpHandler(router);
        return server;
    }
}
