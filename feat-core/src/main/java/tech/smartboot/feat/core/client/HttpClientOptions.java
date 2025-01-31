package tech.smartboot.feat.core.client;

import org.smartboot.socket.buffer.BufferPagePool;
import org.smartboot.socket.extension.plugins.Plugin;

import java.util.List;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/2/13
 */
public class HttpClientOptions extends ClientOptions<HttpResponse> {


    private boolean https = false;

    public HttpClientOptions(String host, int port) {
        super(host, port);
    }


    /**
     * 设置建立连接的超时时间
     */
    public HttpClientOptions connectTimeout(int connectTimeout) {
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
    public HttpClientOptions proxy(String host, int port, String username, String password) {
        super.proxy(host, port, username, password);
        return this;
    }

    /**
     * 连接代理服务器
     *
     * @param host 代理服务器地址
     * @param port 代理服务器端口
     */
    public HttpClientOptions proxy(String host, int port) {
        return this.proxy(host, port, null, null);
    }


    public HttpClientOptions readBufferSize(int readBufferSize) {
        super.readBufferSize(readBufferSize);
        return this;
    }

    public HttpClientOptions readBufferPool(BufferPagePool readBufferPool) {
        super.readBufferPool(readBufferPool);
        return this;
    }

    public HttpClientOptions writeBufferPool(BufferPagePool writeBufferPool) {
        super.writeBufferPool(writeBufferPool);
        return this;
    }

    /**
     * 启用 debug 模式后会打印码流
     */
    public HttpClientOptions debug(boolean debug) {
        super.debug(debug);
        return this;
    }

    public HttpClientOptions addPlugin(Plugin<HttpResponse> plugin) {
        super.addPlugin(plugin);
        return this;
    }

    public List<Plugin<HttpResponse>> getPlugins() {
        return super.getPlugins();
    }

    public boolean isHttps() {
        return https;
    }

    public HttpClientOptions setHttps(boolean https) {
        this.https = https;
        return this;
    }
}
