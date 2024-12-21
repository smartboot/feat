package tech.smartboot.feat.core.client;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/2/13
 */
class ProxyConfig {
    /**
     * 代理服务器地址
     */
    private final String proxyHost;
    /**
     * 代理服务器端口
     */
    private final int proxyPort;
    /**
     * 代理服务器授权账户
     */
    private final String proxyUserName;
    /**
     * 代理服务器授权密码
     */
    private final String proxyPassword;

    public ProxyConfig(String proxyHost, int proxyPort, String proxyUserName, String proxyPassword) {
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
        this.proxyUserName = proxyUserName;
        this.proxyPassword = proxyPassword;
    }

    public String getProxyHost() {
        return proxyHost;
    }

    public int getProxyPort() {
        return proxyPort;
    }

    public String getProxyUserName() {
        return proxyUserName;
    }

    public String getProxyPassword() {
        return proxyPassword;
    }
}
