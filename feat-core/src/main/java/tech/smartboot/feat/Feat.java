package tech.smartboot.feat;

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

}
