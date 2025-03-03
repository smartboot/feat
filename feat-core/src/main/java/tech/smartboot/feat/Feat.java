/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat;

import com.alibaba.fastjson2.JSON;
import tech.smartboot.feat.core.client.Header;
import tech.smartboot.feat.core.client.HttpClient;
import tech.smartboot.feat.core.client.HttpOptions;
import tech.smartboot.feat.core.client.HttpPost;
import tech.smartboot.feat.core.client.WebSocketClient;
import tech.smartboot.feat.core.client.WebSocketListener;
import tech.smartboot.feat.core.client.WebSocketOptions;
import tech.smartboot.feat.core.common.HeaderValue;
import tech.smartboot.feat.core.server.HttpServer;
import tech.smartboot.feat.core.server.ServerOptions;
import tech.smartboot.feat.fileserver.FileServerOptions;
import tech.smartboot.feat.fileserver.HttpStaticResourceHandler;

import java.io.IOException;
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

    public static HttpPost postJson(String api, Object body) {
        return postJson(api, h -> {
        }, body);
    }

    public static HttpPost postJson(String api, Consumer<Header> header, Object body) {
        return postJson(api, options -> {
        }, header, body);
    }


    public static HttpClient httpClient(String baseUrl, Consumer<HttpOptions> options) {
        HttpClient httpClient = new HttpClient(baseUrl);
        options.accept(httpClient.options());
        return httpClient;
    }

    public static HttpPost postJson(String api, Consumer<HttpOptions> options, Consumer<Header> header, Object body) {
        HttpClient httpClient = httpClient(api, options);
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

    public static WebSocketClient websocket(String url, WebSocketListener listener) throws IOException {
        return websocket(url, opts -> {
        }, listener);
    }

    public static WebSocketClient websocket(String url, Consumer<WebSocketOptions> options, WebSocketListener listener) throws IOException {
        WebSocketClient webSocketClient = new WebSocketClient(url);
        options.accept(webSocketClient.options());
        webSocketClient.connect(listener);
        return webSocketClient;
    }
}
