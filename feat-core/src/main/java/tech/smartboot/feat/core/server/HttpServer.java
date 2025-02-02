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
import tech.smartboot.feat.core.common.FeatUtils;
import tech.smartboot.feat.core.common.HeaderValue;
import tech.smartboot.feat.core.common.enums.HeaderNameEnum;
import tech.smartboot.feat.core.common.enums.HttpMethodEnum;
import tech.smartboot.feat.core.common.enums.HttpProtocolEnum;
import tech.smartboot.feat.core.server.handler.BaseHttpHandler;
import tech.smartboot.feat.core.server.impl.HttpMessageProcessor;
import tech.smartboot.feat.core.server.impl.HttpRequestProtocol;

public class HttpServer {
    /**
     * http://patorjk.com/software/taag/
     * Font Name: Varsity
     */


    /**
     * http消息解码器
     */
    private final HttpMessageProcessor processor;
    private final ServerOptions options;
    private final HttpRequestProtocol protocol;
    private AioQuickServer server;
    private boolean started = false;
    private BufferPagePool bufferPagePool;

    public HttpServer() {
        this(new ServerOptions());
    }

    public HttpServer(ServerOptions options) {
        this.options = options;
        this.processor = new HttpMessageProcessor(options);
        this.protocol = new HttpRequestProtocol(options);
    }

    public HttpServer httpHandler(HttpHandler handler) {
        if (handler instanceof BaseHttpHandler) {
            processor.httpServerHandler((BaseHttpHandler) handler);
        } else {
            processor.httpServerHandler(new BaseHttpHandler() {
                @Override
                public void handle(HttpRequest request) throws Throwable {
                    handler.handle(request);
                }
            });
        }
        return this;
    }


    /**
     * 服务配置
     */
    public final ServerOptions options() {
        return options;
    }

    public final void listen() {
        listen(8080);
    }

    public final void listen(int port) {
        listen(null, port);
    }

    /**
     * 启动HTTP服务
     */
    public synchronized void listen(String host, int port) {
        if (started) {
            throw new RuntimeException("server is running");
        }
        started = true;
        bufferPagePool = new BufferPagePool(options.getThreadNum(), true);
        initByteCache();

        options.getPlugins().forEach(processor::addPlugin);

        server = new AioQuickServer(host, port, protocol, processor);
        server.setThreadNum(options.getThreadNum()).setBannerEnabled(false).setReadBufferSize(options.getReadBufferSize()).setBufferPagePool(bufferPagePool, bufferPagePool).setWriteBuffer(options.getWriteBufferSize(), 16);
        if (!options.isLowMemory()) {
            server.disableLowMemory();
        }
        try {
            if (options.group() == null) {
                server.start();
            } else {
                server.start(options.group());
            }

            if (options.isBannerEnabled()) {
                System.out.println(FeatUtils.getResourceAsString("feat-banner.txt") + "\r\n :: Feat :: (" + ServerOptions.VERSION + ")");
                System.out.println(FeatUtils.getResourceAsString("feat-support.txt"));
                System.out.println("\u001B[32m\uD83C\uDF89Congratulations, the feat startup is successful" + ".\u001B[0m");
            }
        } catch (Throwable e) {
            System.out.println("\u001B[31m❗feat has failed to start for some reason.\u001B[0m");
            throw new RuntimeException("server start error.", e);
        }
    }

    private void initByteCache() {
        for (HttpMethodEnum httpMethodEnum : HttpMethodEnum.values()) {
            options.getByteCache().addNode(httpMethodEnum.getMethod());
        }
        for (HttpProtocolEnum httpProtocolEnum : HttpProtocolEnum.values()) {
            options.getByteCache().addNode(httpProtocolEnum.getProtocol(), httpProtocolEnum);
        }
        for (HeaderNameEnum headerNameEnum : HeaderNameEnum.values()) {
            options.getHeaderNameByteTree().addNode(headerNameEnum.getName(), headerNameEnum);
        }
        // 缓存一些常用字符串
        options.getByteCache().addNode(HeaderValue.TransferEncoding.CHUNKED);
        options.getByteCache().addNode(HeaderValue.ContentEncoding.GZIP);
        options.getByteCache().addNode(HeaderValue.Connection.UPGRADE);
        options.getByteCache().addNode(HeaderValue.Connection.KEEPALIVE);
        options.getByteCache().addNode(HeaderValue.ContentType.MULTIPART_FORM_DATA);
        options.getByteCache().addNode(HeaderValue.ContentType.APPLICATION_JSON);
        options.getByteCache().addNode(HeaderValue.ContentType.X_WWW_FORM_URLENCODED);
        options.getByteCache().addNode(HeaderValue.ContentType.APPLICATION_JSON_UTF8);
        options.getByteCache().addNode(HeaderValue.ContentType.TEXT_HTML_UTF8);
        options.getByteCache().addNode(HeaderValue.ContentType.TEXT_PLAIN_UTF8);
    }

    /**
     * 停止服务
     */
    public void shutdown() {
        if (server != null) {
            Runnable runnable = options.shutdownHook();
            if (runnable != null) {
                try {
                    runnable.run();
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
            server.shutdown();
            server = null;
            bufferPagePool.release();
            bufferPagePool = null;
        }
    }
}
