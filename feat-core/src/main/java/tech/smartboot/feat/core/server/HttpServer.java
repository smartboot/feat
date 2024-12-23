/*******************************************************************************
 * Copyright (c) 2024, tech.smartboot. All rights reserved.
 * project name: feat
 * file name: HttpBootstrap.java
 * Date: 2021-02-07
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package tech.smartboot.feat.core.server;

import org.smartboot.socket.buffer.BufferPagePool;
import org.smartboot.socket.transport.AioQuickServer;
import tech.smartboot.feat.core.common.enums.HeaderNameEnum;
import tech.smartboot.feat.core.common.enums.HeaderValueEnum;
import tech.smartboot.feat.core.common.enums.HttpMethodEnum;
import tech.smartboot.feat.core.common.enums.HttpProtocolEnum;
import tech.smartboot.feat.core.server.impl.HttpMessageProcessor;
import tech.smartboot.feat.core.server.impl.HttpRequestProtocol;

import java.util.concurrent.CompletableFuture;

public class HttpServer {
    /**
     * http://patorjk.com/software/taag/
     * Font Name: Varsity
     */
    private static final String BANNER =
            " ________   ________        _        _________  \n" +
            "|_   __  | |_   __  |      / \\      |  _   _  | \n" +
            "  | |_ \\_|   | |_ \\_|     / _ \\     |_/ | | \\_| \n" +
            "  |  _|      |  _| _     / ___ \\        | |     \n" +
            " _| |_      _| |__/ |  _/ /   \\ \\_     _| |_    \n" +
            "|_____|    |________| |____| |____|   |_____|   \n";


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
    private BufferPagePool bufferPagePool;

    public HttpServer() {
        this(new HttpMessageProcessor());
    }

    public HttpServer(HttpMessageProcessor processor) {
        this.processor = processor;
        this.processor.setConfiguration(configuration);
    }

    /**
     * Http服务端口号
     */
    public HttpServer setPort(int port) {
        this.port = port;
        return this;
    }

    /**
     * 往 http 处理器管道中注册 Handle
     *
     * @param httpHandler
     * @return
     */
    public HttpServer httpHandler(HttpServerHandler httpHandler) {
        processor.httpServerHandler(httpHandler);
        processor.http2ServerHandler(new Http2ServerHandler() {

            @Override
            public void handle(HttpRequest request, HttpResponse response, CompletableFuture<Object> completableFuture) throws Throwable {
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
    public HttpServer webSocketHandler(WebSocketHandler webSocketHandler) {
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
        bufferPagePool = new BufferPagePool(configuration.getThreadNum(), true);
        initByteCache();

        configuration.getPlugins().forEach(processor::addPlugin);

        server = new AioQuickServer(configuration.getHost(), port, protocol, processor);
        server.setThreadNum(configuration.getThreadNum()).setBannerEnabled(false).setReadBufferSize(configuration.getReadBufferSize()).setBufferPagePool(bufferPagePool,
                bufferPagePool).setWriteBuffer(configuration.getWriteBufferSize(), 16);
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
                System.out.println(BANNER + "\r\n :: Feat :: (" + HttpServerConfiguration.VERSION + ")");
                System.out.println("Technical Support:");
                System.out.println(" - Document: https://smartboot.tech]");
//                System.out.println(" - Gitee: https://gitee.com/smartboot/feat");
                System.out.println(" - Github: https://github.com/smartboot/feat");
                System.out.println("\u001B[32m\uD83C\uDF89Congratulations, the feat startup is successful" + ".\u001B[0m");
            }
        } catch (Throwable e) {
            System.out.println("\u001B[31m❗feat has failed to start for some reason.\u001B[0m");
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
            bufferPagePool.release();
            bufferPagePool = null;
        }
    }
}
