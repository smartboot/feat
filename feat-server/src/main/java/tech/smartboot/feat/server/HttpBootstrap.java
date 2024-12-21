/*******************************************************************************
 * Copyright (c) 2017-2021, org.smartboot. All rights reserved.
 * project name: smart-http
 * file name: HttpBootstrap.java
 * Date: 2021-02-07
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package tech.smartboot.feat.server;

import tech.smartboot.feat.common.enums.HeaderNameEnum;
import tech.smartboot.feat.common.enums.HeaderValueEnum;
import tech.smartboot.feat.common.enums.HttpMethodEnum;
import tech.smartboot.feat.common.enums.HttpProtocolEnum;
import tech.smartboot.feat.server.impl.HttpMessageProcessor;
import tech.smartboot.feat.server.impl.HttpRequestProtocol;
import org.smartboot.socket.transport.AioQuickServer;

import java.util.concurrent.CompletableFuture;

public class HttpBootstrap {

    private static final String BANNER = "                               _       _      _    _          \n" +
            "                              ( )_    ( )    ( )_ ( )_        \n" +
            "  ___   ___ ___     _ _  _ __ | ,_)   | |__  | ,_)| ,_) _ _   \n" +
            "/',__)/' _ ` _ `\\ /'_` )( '__)| |     |  _ `\\| |  | |  ( '_`\\ \n" +
            "\\__, \\| ( ) ( ) |( (_| || |   | |_    | | | || |_ | |_ | (_) )\n" +
            "(____/(_) (_) (_)`\\__,_)(_)   `\\__)   (_) (_)`\\__)`\\__)| ,__/'\n" +
            "                                                       | |    \n" +
            "                                                       (_)   ";


    /**
     * http消息解码器
     */
    private final HttpMessageProcessor processor;
    private final HttpServerConfiguration configuration = new HttpServerConfiguration();
    private final HttpRequestProtocol protocol = new HttpRequestProtocol(configuration);
    private AioQuickServer server;
    /**
     * Http服务端口号
     */
    private int port = 8080;
    private boolean started = false;

    public HttpBootstrap() {
        this(new HttpMessageProcessor());
    }

    public HttpBootstrap(HttpMessageProcessor processor) {
        this.processor = processor;
        this.processor.setConfiguration(configuration);
    }

    /**
     * Http服务端口号
     */
    public HttpBootstrap setPort(int port) {
        this.port = port;
        return this;
    }

    /**
     * 往 http 处理器管道中注册 Handle
     *
     * @param httpHandler
     * @return
     */
    public HttpBootstrap httpHandler(HttpServerHandler httpHandler) {
        processor.httpServerHandler(httpHandler);
        processor.http2ServerHandler(new Http2ServerHandler() {

            @Override
            public void handle(HttpRequest request, HttpResponse response,
                               CompletableFuture<Object> completableFuture) throws Throwable {
                httpHandler.handle(request, response, completableFuture);
            }
        });
        return this;
    }

//    public HttpBootstrap http2Handler(Http2ServerHandler httpHandler) {
//        processor.http2ServerHandler(httpHandler);
//        return this;
//    }

    /**
     * 获取websocket的处理器管道
     *
     * @return
     */
    public HttpBootstrap webSocketHandler(WebSocketHandler webSocketHandler) {
        processor.setWebSocketHandler(webSocketHandler);
        return this;
    }

    /**
     * 服务配置
     *
     * @return
     */
    public HttpServerConfiguration configuration() {
        return configuration;
    }

    /**
     * 启动HTTP服务
     *
     * @throws RuntimeException
     */
    public synchronized void start() {
        if (started) {
            throw new RuntimeException("server is running");
        }
        started = true;
        initByteCache();

        configuration.getPlugins().forEach(processor::addPlugin);

        server = new AioQuickServer(configuration.getHost(), port, protocol, processor);
        server.setThreadNum(configuration.getThreadNum())
                .setBannerEnabled(false)
                .setReadBufferSize(configuration.getReadBufferSize())
                .setBufferPagePool(configuration.getReadBufferPool(), configuration.getWriteBufferPool())
                .setWriteBuffer(configuration.getWriteBufferSize(), 16);
        if (!configuration.isLowMemory()) {
            server.disableLowMemory();
        }
        try {
            if (configuration.group() == null) {
                server.start();
            } else {
                server.start(configuration.group());
            }

            if (configuration.isBannerEnabled()) {
                System.out.println(BANNER + "\r\n :: smart-http :: (" + HttpServerConfiguration.VERSION + ")");
                System.out.println("Technical Support:");
                System.out.println(" - Document: https://smartboot.tech]");
                System.out.println(" - Gitee: https://gitee.com/smartboot/smart-http");
                System.out.println(" - Github: https://github.com/smartboot/smart-http");
                System.out.println("\u001B[32m\uD83C\uDF89Congratulations, the smart-http startup is successful" +
                        ".\u001B[0m");
            }
        } catch (Throwable e) {
            System.out.println("\u001B[31m❗smart-http has failed to start for some reason.\u001B[0m");
            throw new RuntimeException("server start error.", e);
        }
    }

    private void initByteCache() {
        for (HttpMethodEnum httpMethodEnum : HttpMethodEnum.values()) {
            configuration.getByteCache().addNode(httpMethodEnum.getMethod());
        }
        for (HttpProtocolEnum httpProtocolEnum : HttpProtocolEnum.values()) {
            configuration.getByteCache().addNode(httpProtocolEnum.getProtocol(), httpProtocolEnum);
        }
        for (HeaderNameEnum headerNameEnum : HeaderNameEnum.values()) {
            configuration.getHeaderNameByteTree().addNode(headerNameEnum.getName(), headerNameEnum);
        }
        for (HeaderValueEnum headerValueEnum : HeaderValueEnum.values()) {
            configuration.getByteCache().addNode(headerValueEnum.getName());
        }
    }

    /**
     * 停止服务
     */
    public void shutdown() {
        if (server != null) {
            server.shutdown();
            server = null;
        }
    }
}
