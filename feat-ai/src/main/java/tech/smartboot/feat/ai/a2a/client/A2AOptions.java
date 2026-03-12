/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.a2a.client;

import com.alibaba.fastjson2.JSONObject;
import tech.smartboot.feat.ai.Vendor;

/**
 * A2A 客户端配置选项类
 *
 * <p>用于配置A2A客户端的各项参数，包括端点URL、超时时间、认证信息等。</p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0 3/12/26
 */
public class A2AOptions {
    /**
     * A2A服务端点URL
     */
    private String endpoint;

    /**
     * 连接超时时间（毫秒）
     */
    private int connectTimeout = 10000;

    /**
     * 读取超时时间（毫秒）
     */
    private int readTimeout = 60000;

    /**
     * API Key
     */
    private String apiKey;

    /**
     * Bearer Token
     */
    private String bearerToken;

    /**
     * 请求头
     */
    private JSONObject headers;

    /**
     * 是否启用调试日志
     */
    private boolean debug;

    /**
     * 供应商配置
     */
    private Vendor vendor;

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBearerToken() {
        return bearerToken;
    }

    public void setBearerToken(String bearerToken) {
        this.bearerToken = bearerToken;
    }

    public JSONObject getHeaders() {
        return headers;
    }

    public void setHeaders(JSONObject headers) {
        this.headers = headers;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public Vendor getVendor() {
        return vendor;
    }

    public void setVendor(Vendor vendor) {
        this.vendor = vendor;
    }

    /**
     * 添加自定义请求头
     *
     * @param name  请求头名称
     * @param value 请求头值
     * @return 当前A2AOptions实例（链式调用）
     */
    public A2AOptions addHeader(String name, String value) {
        if (this.headers == null) {
            this.headers = new JSONObject();
        }
        this.headers.put(name, value);
        return this;
    }
}
