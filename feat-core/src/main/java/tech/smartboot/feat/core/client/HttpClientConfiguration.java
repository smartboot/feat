package tech.smartboot.feat.core.client;

import org.smartboot.socket.buffer.BufferPagePool;
import org.smartboot.socket.extension.plugins.Plugin;

import java.util.List;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/2/13
 */
public class HttpClientConfiguration extends ClientConfiguration<HttpResponse> {


    private boolean https = false;

    public HttpClientConfiguration(String host, int port) {
        super(host, port);
    }


    /**
     * 设置建立连接的超时时间
     */
    public HttpClientConfiguration connectTimeout(int connectTimeout) {
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
    public HttpClientConfiguration proxy(String host, int port, String username, String password) {
        super.proxy(host, port, username, password);
        return this;
    }

    /**
     * 连接代理服务器
     *
     * @param host 代理服务器地址
     * @param port 代理服务器端口
     */
    public HttpClientConfiguration proxy(String host, int port) {
        return this.proxy(host, port, null, null);
    }


    public HttpClientConfiguration readBufferSize(int readBufferSize) {
        super.readBufferSize(readBufferSize);
        return this;
    }

    public HttpClientConfiguration readBufferPool(BufferPagePool readBufferPool) {
        super.readBufferPool(readBufferPool);
        return this;
    }

    public HttpClientConfiguration writeBufferPool(BufferPagePool writeBufferPool) {
        super.writeBufferPool(writeBufferPool);
        return this;
    }

    /**
     * 启用 debug 模式后会打印码流
     */
    public HttpClientConfiguration debug(boolean debug) {
        super.debug(debug);
        return this;
    }

    public HttpClientConfiguration addPlugin(Plugin<HttpResponse> plugin) {
        super.addPlugin(plugin);
        return this;
    }

    public List<Plugin<HttpResponse>> getPlugins() {
        return super.getPlugins();
    }

    public boolean isHttps() {
        return https;
    }

    public HttpClientConfiguration setHttps(boolean https) {
        this.https = https;
        return this;
    }
}
