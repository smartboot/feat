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

public class HttpServer {
    /**
     * http://patorjk.com/software/taag/
     * Font Name: Varsity
     */
    private static final String BANNER =
            " ________   ________        _        _________  \n" + "|_   __  | |_   __  |      / \\      |  _   _  | \n" + "  | |_ \\_|   | |_ \\_|     / _ \\     |_/ " + "|" + " | \\_| \n" + "  | "
                    + " _|      |  _| _     / ___ \\        | |     \n" + " _| |_      _| |__/ |  _/ /   \\ \\_     _| |_    \n" + "|_____|    |________| |____| |____|   |_____|   \n";


    /**
     * http消息解码器
     */
    private final HttpMessageProcessor processor;
    private final FeatOptions options;
    private final HttpRequestProtocol protocol;
    private AioQuickServer server;
    private boolean started = false;
    private BufferPagePool bufferPagePool;

    public HttpServer() {
        this(new FeatOptions());
    }

    public HttpServer(FeatOptions options) {
        this.options = options;
        this.processor = new HttpMessageProcessor(options);
        this.protocol = new HttpRequestProtocol(options);
    }

    /**
     * 往 http 处理器管道中注册 Handle
     *
     * @param httpHandler
     * @return
     */
    public HttpServer httpHandler(HttpServerHandler httpHandler) {
        processor.httpServerHandler(httpHandler);
        return this;
    }


    /**
     * 服务配置
     *
     * @return
     */
    public FeatOptions options() {
        return options;
    }

    public void listen() {
        listen(8080);
    }

    public void listen(int port) {
        listen(null, port);
    }

    /**
     * 启动HTTP服务
     *
     * @throws RuntimeException
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
                System.out.println(BANNER + "\r\n :: Feat :: (" + FeatOptions.VERSION + ")");
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
            options.getByteCache().addNode(httpMethodEnum.getMethod());
        }
        for (HttpProtocolEnum httpProtocolEnum : HttpProtocolEnum.values()) {
            options.getByteCache().addNode(httpProtocolEnum.getProtocol(), httpProtocolEnum);
        }
        for (HeaderNameEnum headerNameEnum : HeaderNameEnum.values()) {
            options.getHeaderNameByteTree().addNode(headerNameEnum.getName(), headerNameEnum);
        }
        // 缓存一些常用字符串
        options.getByteCache().addNode(HeaderValueEnum.TransferEncoding.CHUNKED);
        options.getByteCache().addNode(HeaderValueEnum.ContentEncoding.GZIP);
        options.getByteCache().addNode(HeaderValueEnum.Connection.UPGRADE);
        options.getByteCache().addNode(HeaderValueEnum.Connection.KEEPALIVE);
        options.getByteCache().addNode(HeaderValueEnum.ContentType.MULTIPART_FORM_DATA);
        options.getByteCache().addNode(HeaderValueEnum.ContentType.APPLICATION_JSON);
        options.getByteCache().addNode(HeaderValueEnum.ContentType.X_WWW_FORM_URLENCODED);
        options.getByteCache().addNode(HeaderValueEnum.ContentType.APPLICATION_JSON_UTF8);
        options.getByteCache().addNode(HeaderValueEnum.ContentType.TEXT_HTML_UTF8);
        options.getByteCache().addNode(HeaderValueEnum.ContentType.TEXT_PLAIN_UTF8);
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
