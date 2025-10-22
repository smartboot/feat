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

import java.nio.channels.AsynchronousChannelGroup;
import java.util.ArrayList;
import java.util.List;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class WebSocketOptions extends Options<WebSocketResponse> {
    /**
     * 连接超时时间
     */
    private int connectTimeout;
    /**
     * smart-socket 插件
     */
    private final List<Plugin<WebSocketResponse>> plugins = new ArrayList<>();

    private boolean wss = false;
    /**
     * read缓冲区大小
     */
    private int readBufferSize = 1024;
    /**
     * 绑定线程池资源组
     */
    private AsynchronousChannelGroup group;

    public WebSocketOptions(String host, int port) {
        super(host, port);
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }


    /**
     * 设置建立连接的超时时间
     */
    public WebSocketOptions connectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
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
    public WebSocketOptions proxy(String host, int port, String username, String password) {
        super.proxy(host, port, username, password);
        return this;
    }

    /**
     * 连接代理服务器
     *
     * @param host 代理服务器地址
     * @param port 代理服务器端口
     */
    public WebSocketOptions proxy(String host, int port) {
        return this.proxy(host, port, null, null);
    }


    public WebSocketOptions readBufferSize(int readBufferSize) {
        this.readBufferSize = readBufferSize;
        return this;
    }

    public int readBufferSize() {
        return readBufferSize;
    }

    /**
     * 启用 debug 模式后会打印码流
     */
    public WebSocketOptions debug(boolean debug) {
        if (debug) {
            this.addPlugin(new StreamMonitorPlugin<>());
        }
        return this;
    }

    public WebSocketOptions addPlugin(Plugin<WebSocketResponse> plugin) {
        plugins.add(plugin);
        return this;
    }

    public List<Plugin<WebSocketResponse>> getPlugins() {
        return plugins;
    }

    public boolean isWss() {
        return wss;
    }

    public WebSocketOptions setWss(boolean wss) {
        this.wss = wss;
        return this;
    }

    public WebSocketOptions group(AsynchronousChannelGroup group) {
        this.group = group;
        return this;
    }

    public AsynchronousChannelGroup group() {
        return group;
    }
}
