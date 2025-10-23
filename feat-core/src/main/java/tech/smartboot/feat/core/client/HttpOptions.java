/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.core.client;

import org.smartboot.socket.extension.plugins.Plugin;
import org.smartboot.socket.extension.plugins.StreamMonitorPlugin;
import org.smartboot.socket.transport.MultiplexClient;

import java.nio.channels.AsynchronousChannelGroup;
import java.util.Map;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public final class HttpOptions extends Options<HttpResponse> {

    private final MultiplexClient<AbstractResponse>.Options multiplexOptions;

    HttpOptions(MultiplexClient<AbstractResponse>.Options multiplexOptions, String host, int port) {
        super(host, port);
        this.multiplexOptions = multiplexOptions;
        multiplexOptions.setHost(host);
        multiplexOptions.setPort(port);
        //消息处理器
        HttpMessageProcessor processor = new HttpMessageProcessor();
        multiplexOptions.init(processor, processor);
    }


    /**
     * 设置建立连接的超时时间
     */
    public HttpOptions connectTimeout(int connectTimeout) {
        multiplexOptions.setConnectTimeout(connectTimeout);
        return this;
    }

    /**
     * 设置 Http 代理服务器
     *
     * @param host     代理服务器地址
     * @param port     代理服务器端口
     * @param username 授权账户
     * @param password 授权密码
     */
    public HttpOptions proxy(String host, int port, String username, String password) {
        multiplexOptions.setHost(host);
        multiplexOptions.setPort(port);
        super.proxy(host, port, username, password);
        return this;
    }

    /**
     * 连接代理服务器
     *
     * @param host 代理服务器地址
     * @param port 代理服务器端口
     */
    public HttpOptions proxy(String host, int port) {
        return this.proxy(host, port, null, null);
    }


    public HttpOptions readBufferSize(int readBufferSize) {
        multiplexOptions.setReadBuffer(readBufferSize);
        return this;
    }

    public HttpOptions setWriteBufferSize(int writeBufferSize) {
        multiplexOptions.setWriteBuffer(writeBufferSize, 2);
        return this;
    }

    /**
     * 启用 debug 模式后会打印码流
     */
    public HttpOptions debug(boolean debug) {
        if (debug) {
            addPlugin(new StreamMonitorPlugin<>(StreamMonitorPlugin.BLUE_TEXT_INPUT_STREAM, StreamMonitorPlugin.RED_TEXT_OUTPUT_STREAM));
        }
        return this;
    }

    public HttpOptions addPlugin(Plugin<HttpResponse> plugin) {
        Plugin p = plugin;
        multiplexOptions.addPlugin(p);
        return this;
    }


    HttpOptions setHttps(boolean https) {
        multiplexOptions.setSsl(https);
        return this;
    }

    public HttpOptions group(AsynchronousChannelGroup group) {
        multiplexOptions.group(group);
        return this;
    }

    public HttpOptions idleTimeout(int idleTimeout) {
        multiplexOptions.idleTimeout(idleTimeout);
        return this;
    }

    @Override
    public HttpOptions setHeaders(Map<String, String> headers) {
        super.setHeaders(headers);
        return this;
    }

    @Override
    public HttpOptions addHeader(String name, String value) {
        super.addHeader(name, value);
        return this;
    }
}
