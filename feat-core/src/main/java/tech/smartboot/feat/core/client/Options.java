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

import java.nio.channels.AsynchronousChannelGroup;
import java.util.ArrayList;
import java.util.List;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
class Options<T> {


    /**
     * smart-socket 插件
     */
    private final List<Plugin<T>> plugins = new ArrayList<>();

    /**
     * 连接超时时间
     */
    private int connectTimeout;

    /**
     * 远程地址
     */
    private final String host;
    /**
     * 远程端口
     */
    private final int port;

    private ProxyOptions proxy;

    /**
     * read缓冲区大小
     */
    private int readBufferSize = 1024;

    /**
     * read缓冲区大小
     */
    private int writeBufferSize = 1024;


    private boolean https = false;

    private boolean debug = false;

    /**
     * 空闲超时时间，单位：毫秒
     */
    private int idleTimeout = 60000;
    /**
     * 绑定线程池资源组
     */
    private AsynchronousChannelGroup group;

    public Options(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    /**
     * 设置建立连接的超时时间
     */
    protected Options<T> connectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
        return this;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }


    /**
     * 设置 Http 代理服务器
     *
     * @param host     代理服务器地址
     * @param port     代理服务器端口
     * @param username 授权账户
     * @param password 授权密码
     */
    protected Options<T> proxy(String host, int port, String username, String password) {
        this.proxy = new ProxyOptions(host, port, username, password);
        return this;
    }

    /**
     * 连接代理服务器
     *
     * @param host 代理服务器地址
     * @param port 代理服务器端口
     */
    public Options<T> proxy(String host, int port) {
        return this.proxy(host, port, null, null);
    }

    ProxyOptions getProxy() {
        return proxy;
    }

    public int readBufferSize() {
        return readBufferSize;
    }

    protected Options<T> readBufferSize(int readBufferSize) {
        this.readBufferSize = readBufferSize;
        return this;
    }

    public int getWriteBufferSize() {
        return writeBufferSize;
    }

    public Options<T> setWriteBufferSize(int writeBufferSize) {
        this.writeBufferSize = writeBufferSize;
        return this;
    }

    /**
     * 启用 debug 模式后会打印码流
     */
    protected Options<T> debug(boolean debug) {
        this.debug = debug;
        return this;
    }

    boolean isDebug() {
        return debug;
    }

    protected Options<T> addPlugin(Plugin<T> plugin) {
        plugins.add(plugin);
        return this;
    }

    public List<Plugin<T>> getPlugins() {
        return plugins;
    }

    final boolean isHttps() {
        return https;
    }

    Options<T> setHttps(boolean https) {
        this.https = https;
        return this;
    }


    public Options<T> group(AsynchronousChannelGroup group) {
        this.group = group;
        return this;
    }

    public AsynchronousChannelGroup group() {
        return group;
    }

    public int idleTimeout() {
        return idleTimeout;
    }

    public Options<T> idleTimeout(int idleTimeout) {
        this.idleTimeout = idleTimeout;
        return this;
    }
}
