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

import java.util.HashMap;
import java.util.Map;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
class Options<T> {


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
     * 自定义请求头
     */
    private final Map<String, String> headers = new HashMap<>();

    public Options(String host, int port) {
        this.host = host;
        this.port = port;
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


    public final Map<String, String> getHeaders() {
        return headers;
    }

    public Options<T> setHeaders(Map<String, String> headers) {
        this.headers.putAll(headers);
        return this;
    }

    /**
     * 添加请求头
     *
     * @param name  头名称
     * @param value 头值
     * @return this
     */
    public Options<T> addHeader(String name, String value) {
        this.headers.put(name, value);
        return this;
    }
}
