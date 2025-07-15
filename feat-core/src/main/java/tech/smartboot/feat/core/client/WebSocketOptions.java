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

import java.util.List;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class WebSocketOptions extends Options<WebSocketResponse> {


    private boolean wss = false;

    public WebSocketOptions(String host, int port) {
        super(host, port);
    }


    /**
     * 设置建立连接的超时时间
     */
    public WebSocketOptions connectTimeout(int connectTimeout) {
        super.connectTimeout(connectTimeout);
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
        super.readBufferSize(readBufferSize);
        return this;
    }

    /**
     * 启用 debug 模式后会打印码流
     */
    public WebSocketOptions debug(boolean debug) {
        super.debug(debug);
        return this;
    }

    public WebSocketOptions addPlugin(Plugin<WebSocketResponse> plugin) {
        super.addPlugin(plugin);
        return this;
    }

    public List<Plugin<WebSocketResponse>> getPlugins() {
        return super.getPlugins();
    }

    public boolean isWss() {
        return wss;
    }

    public WebSocketOptions setWss(boolean wss) {
        this.wss = wss;
        return this;
    }
}
