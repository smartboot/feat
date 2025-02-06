package tech.smartboot.feat;

import com.alibaba.fastjson2.JSON;
import tech.smartboot.feat.cloud.ApplicationContext;
import tech.smartboot.feat.cloud.CloudOptions;
import tech.smartboot.feat.core.client.Header;
import tech.smartboot.feat.core.client.HttpClient;
import tech.smartboot.feat.core.client.HttpOptions;
import tech.smartboot.feat.core.client.HttpPost;
import tech.smartboot.feat.core.common.HeaderValue;
import tech.smartboot.feat.core.common.exception.FeatException;
import tech.smartboot.feat.core.server.HttpServer;
import tech.smartboot.feat.core.server.ServerOptions;
import tech.smartboot.feat.fileserver.FileServerOptions;
import tech.smartboot.feat.fileserver.HttpStaticResourceHandler;

import java.util.function.Consumer;

public class Feat {
    public static HttpServer httpServer() {
        return httpServer(new ServerOptions());
    }

    public static HttpServer httpServer(ServerOptions options) {
        return new HttpServer(options);
    }

    public static HttpServer httpServer(Consumer<ServerOptions> options) {
        ServerOptions opt = new ServerOptions();
        options.accept(opt);
        return httpServer(opt);
    }

    public static HttpServer fileServer(Consumer<FileServerOptions> options) {
        FileServerOptions opt = new FileServerOptions();
        options.accept(opt);
        return httpServer(opt).httpHandler(new HttpStaticResourceHandler(opt));
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

        HttpServer server = httpServer(opt);
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

    public static HttpPost postJson(String api, Object body) {
        return postJson(api, h -> {
        }, body);
    }

    public static HttpPost postJson(String api, Consumer<Header> header, Object body) {
        return postJson(api, options -> {
        }, header, body);
    }

    public static HttpPost postJson(String api, Consumer<HttpOptions> options, Consumer<Header> header, Object body) {
        HttpClient httpClient = new HttpClient(api);
        options.accept(httpClient.options());
        byte[] bytes = JSON.toJSONBytes(body);
        HttpPost post = httpClient.post();
        post.header(h -> {
            header.accept(h);
            h.setContentType(HeaderValue.ContentType.APPLICATION_JSON);
            h.setContentLength(bytes.length);
        });
        post.body().write(bytes);
        return post;
    }
}
