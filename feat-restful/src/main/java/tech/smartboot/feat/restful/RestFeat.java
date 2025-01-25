package tech.smartboot.feat.restful;

import tech.smartboot.feat.Feat;
import tech.smartboot.feat.cloud.ApplicationContext;
import tech.smartboot.feat.core.server.HttpServer;
import tech.smartboot.feat.core.server.ServerOptions;

import java.util.function.Consumer;

public class RestFeat {

    public static HttpServer createServer(ApplicationContext application, Consumer<ServerOptions> options, String... packages) {
        HttpServer server = Feat.createHttpServer(options);
        Runnable shutdownHook = server.options().shutdownHook();
        if (shutdownHook != null) {
            server.options().shutdownHook(() -> {
                application.destroy();
                shutdownHook.run();
            });
        } else {
            server.options().shutdownHook(application::destroy);
        }
        server.httpHandler(application.getRouter());
        return server;
    }


    public static HttpServer createServer(Consumer<ServerOptions> options, String... packages) {
        ApplicationContext application = new ApplicationContext(packages);
        application.start();
        return createServer(application, options, packages);
    }

    public static HttpServer createServer(String... packages) {
        return createServer(serverOptions -> {
        }, packages);
    }
}
