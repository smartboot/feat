package tech.smartboot.feat.restful;

import tech.smartboot.feat.core.Feat;
import tech.smartboot.feat.core.apt.ApplicationContext;
import tech.smartboot.feat.core.server.HttpServer;
import tech.smartboot.feat.core.server.ServerOptions;
import tech.smartboot.feat.core.server.handler.Router;

import java.util.function.Consumer;

public class RestFeat {
    public static HttpServer createServer() {
        return createServer(serverOptions -> {
        });
    }

    public static HttpServer createServer(Consumer<ServerOptions> options, String... packages) {
        HttpServer server = Feat.createHttpServer(options);
        ApplicationContext application = new ApplicationContext(packages);
        Router router = new Router();
        application.start(router);
        server.httpHandler(router);
        return server;
    }

    public static HttpServer createServer(String... packages) {
        return createServer(serverOptions -> {
        }, packages);
    }
}
