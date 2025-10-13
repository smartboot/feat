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

import java.io.IOException;
import java.util.function.Consumer;

/**
 * Feat框架的核心工具类，提供HTTP服务器、文件服务器、HTTP客户端和WebSocket等功能的快速构建方法。
 * 该类采用流式API设计，支持链式调用，使用简单直观。主要功能包括：
 * <ul>
 *   <li>HTTP服务器：快速构建高性能HTTP服务器，对标vert.x</li>
 *   <li>文件服务器：静态资源服务器，支持反向代理，对标nginx</li>
 *   <li>HTTP客户端：支持RESTFUL API调用，包含JSON请求等常用场景</li>
 *   <li>WebSocket：提供WebSocket客户端功能</li>
 * </ul>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class Feat {
    /**
     * 当前 Feat 框架版本号
     */
    public static final String VERSION = "v1.3.1";

    /**
     * 创建一个HTTP服务器实例，使用默认配置。
     * 该方法对标vert.x的HTTP服务器功能，提供高性能的HTTP请求处理能力。
     *
     * @return 返回配置完成的HttpServer实例
     */
    public static HttpServer httpServer() {
        return httpServer(new ServerOptions());
    }

    /**
     * 使用指定的服务器选项创建HTTP服务器实例。
     *
     * @param options 服务器配置选项
     * @return 返回配置完成的HttpServer实例
     */
    public static HttpServer httpServer(ServerOptions options) {
        return new HttpServer(options);
    }

    /**
     * 通过配置器函数创建HTTP服务器实例，支持自定义配置。
     *
     * @param options 服务器配置选项消费者函数
     * @return 返回配置完成的HttpServer实例
     */
    public static HttpServer httpServer(Consumer<ServerOptions> options) {
        ServerOptions opt = new ServerOptions();
        options.accept(opt);
        return httpServer(opt);
    }


    /**
     * 创建一个JSON POST请求，使用默认的请求头。
     *
     * @param api  目标API地址
     * @param body 请求体对象，将被序列化为JSON
     * @return 返回配置完成的HttpPost实例
     */
    public static HttpPost postJson(String api, Object body) {
        return postJson(api, h -> {
        }, body);
    }

    /**
     * 创建一个JSON POST请求，支持自定义请求头。
     *
     * @param api    目标API地址
     * @param header 请求头配置函数
     * @param body   请求体对象，将被序列化为JSON
     * @return 返回配置完成的HttpPost实例
     */
    public static HttpPost postJson(String api, Consumer<Header> header, Object body) {
        return postJson(api, options -> {
        }, header, body);
    }

    public static HttpClient httpClient(String baseUrl) {
        return httpClient(baseUrl, options -> {
        });
    }

    /**
     * 创建HTTP客户端实例。
     *
     * @param baseUrl 基础URL地址
     * @param options 客户端配置选项消费者函数
     * @return 返回配置完成的HttpClient实例
     */
    public static HttpClient httpClient(String baseUrl, Consumer<HttpOptions> options) {
        HttpClient httpClient = new HttpClient(baseUrl);
        options.accept(httpClient.options());
        return httpClient;
    }

    /**
     * 创建一个完整配置的JSON POST请求，支持自定义客户端选项和请求头。
     *
     * @param api     目标API地址
     * @param options 客户端配置选项消费者函数
     * @param header  请求头配置函数
     * @param body    请求体对象，将被序列化为JSON
     * @return 返回配置完成的HttpPost实例
     */
    public static HttpPost postJson(String api, Consumer<HttpOptions> options, Consumer<Header> header, Object body) {
        // 创建HTTP客户端
        HttpClient httpClient = httpClient(api, options);
        // 序列化请求体
        byte[] bytes = JSON.toJSONBytes(body);
        HttpPost post = httpClient.post();
        // 配置请求头
        post.header(h -> {
            header.accept(h);
            h.setContentType(HeaderValue.ContentType.APPLICATION_JSON);
            h.setContentLength(bytes.length);
        });
        post.body().write(bytes);
        return post;
    }

    /**
     * 创建一个WebSocket客户端连接，使用默认配置。
     *
     * @param url      WebSocket服务器地址
     * @param listener WebSocket事件监听器
     * @return 返回配置完成的WebSocketClient实例
     * @throws IOException 如果连接过程中发生I/O错误
     */
    public static WebSocketClient websocket(String url, WebSocketListener listener) throws IOException {
        return websocket(url, opts -> {
        }, listener);
    }

    /**
     * 创建一个WebSocket客户端连接，支持自定义配置选项。
     *
     * @param url      WebSocket服务器地址
     * @param options  WebSocket配置选项消费者函数
     * @param listener WebSocket事件监听器
     * @return 返回配置完成的WebSocketClient实例
     * @throws IOException 如果连接过程中发生I/O错误
     */
    public static WebSocketClient websocket(String url, Consumer<WebSocketOptions> options, WebSocketListener listener) throws IOException {
        WebSocketClient webSocketClient = new WebSocketClient(url);
        options.accept(webSocketClient.options());
        webSocketClient.connect(listener);
        return webSocketClient;
    }
}
