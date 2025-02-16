package tech.smartboot.feat.cloud;

import tech.smartboot.feat.Feat;
import tech.smartboot.feat.core.common.exception.FeatException;
import tech.smartboot.feat.core.server.HttpServer;

import java.util.function.Consumer;

public class FeatCloud {

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

        HttpServer server = Feat.httpServer(opt);
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
