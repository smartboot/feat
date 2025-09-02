/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.fileserver;

import java.util.ArrayList;
import java.util.List;

/**
 * 反向代理服务器配置选项
 */
public class ProxyOptions {
    /**
     * 代理规则列表
     */
    private final List<ProxyRule> proxyRules = new ArrayList<>();

    /**
     * 连接超时时间（毫秒）
     */
    private int connectTimeout = 5000;

    /**
     * 读取超时时间（毫秒）
     */
    private int readTimeout = 30000;

    /**
     * 最大连接数
     */
    private int maxConnections = 200;

    /**
     * 添加代理规则
     *
     * @param location 匹配的路径
     * @param upstream 上游服务器URL
     * @return ProxyServerOptions实例
     */
    public ProxyOptions addProxyRule(String location, String upstream) {
        proxyRules.add(new ProxyRule(location, upstream));
        return this;
    }

    public List<ProxyRule> getProxyRules() {
        return proxyRules;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public ProxyOptions setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
        return this;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public ProxyOptions setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
        return this;
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    public ProxyOptions setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
        return this;
    }

    /**
     * 代理规则类
     */
    public static class ProxyRule {
        private final String location;
        private final String upstream;

        public ProxyRule(String location, String upstream) {
            this.location = location;
            this.upstream = upstream;
        }

        public String getLocation() {
            return location;
        }

        public String getUpstream() {
            return upstream;
        }
    }
}