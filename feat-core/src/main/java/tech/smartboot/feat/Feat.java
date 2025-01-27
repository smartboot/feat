package tech.smartboot.feat;

import tech.smartboot.feat.cloud.ApplicationContext;
import tech.smartboot.feat.cloud.CloudOptions;
import tech.smartboot.feat.core.common.exception.FeatException;
import tech.smartboot.feat.core.server.HttpServer;
import tech.smartboot.feat.core.server.ServerOptions;
import tech.smartboot.feat.fileserver.FileServerOptions;
import tech.smartboot.feat.fileserver.HttpStaticResourceHandler;

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

    public static HttpServer fileServer(Consumer<FileServerOptions> options) {
        FileServerOptions opt = new FileServerOptions();
        options.accept(opt);
        return createHttpServer(opt).httpHandler(new HttpStaticResourceHandler(opt));
    }

    public static HttpServer cloudServer() {
        return cloudServer(opts -> {
        });
    }

    public static HttpServer cloudServer(Consumer<CloudOptions> options) {
        CloudOptions opt = new CloudOptions();
        options.accept(opt);
        opt.serverName("feat-cloud");
        ApplicationContext application = new ApplicationContext(opt);
        opt.getExternalBeans().forEach(application::addBean);
        try {
            application.start();
        } catch (Throwable e) {
            throw new FeatException("application start exception", e);
        }

        HttpServer server = Feat.createHttpServer(opt);
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
}
