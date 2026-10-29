/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the Apache-2.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.core.common;

/**
 * 连接地址的解析结果，由 {@link #parse(String)} 一次性拆分出 scheme、host、port 与请求路径。
 * <p>
 * 实现上采用单趟线性扫描，不依赖正则、不依赖 {@link java.net.URI}（后者对未编码的中文、空格、下划线域名过于严格，
 * 会直接抛异常），因此对历史用法保持兼容。
 * <p>
 * 把解析收敛到这里，是为了避免维护方在 Host/Port/Path 的边界判定上反复打补丁，线性扫描天然规避了以下几类问题：
 * <ul>
 *     <li>{@code http://host?a=1}：无路径时查询串被当成 host 的一部分</li>
 *     <li>{@code http://host:8080?next=/a:b}：查询串里的 {@code :}、{@code /} 干扰端口与路径的判定</li>
 *     <li>{@code https://r.jina.ai/https://smartboot.tech}：路径中的 {@code :} 被误判为端口分隔符</li>
 *     <li>{@code http://[::1]:8080/path}：IPv6 字面量</li>
 *     <li>{@code http://host/p?x=1#frag}：fragment 不应出现在请求目标中</li>
 * </ul>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public final class HttpUrl {
    private static final String SCHEMA_WS = "ws";
    private static final String SCHEMA_WSS = "wss";

    /**
     * 协议，例如 http、https、ws、wss
     */
    private final String scheme;
    /**
     * 主机名，不含端口与方括号（IPv6 会去掉 {@code []}）
     */
    private final String host;
    /**
     * 端口，缺省时按协议推导，恒为正数
     */
    private final int port;
    /**
     * 请求目标：path + query，恒以 {@code /} 开头
     */
    private final String path;

    private HttpUrl(String scheme, String host, int port, String path) {
        this.scheme = scheme;
        this.host = host;
        this.port = port;
        this.path = path;
    }

    /**
     * 解析连接地址。
     *
     * @param url 形如 {@code http://host:port/path?query} 的地址
     * @return 解析结果
     * @throws IllegalArgumentException 地址非法时抛出
     */
    public static HttpUrl parse(final String url) {
        if (url == null) {
            throw new IllegalArgumentException("url is null");
        }
        String source = url.trim();
        int schemaIndex = source.indexOf("://");
        if (schemaIndex <= 0) {
            throw new IllegalArgumentException("invalid url:" + url);
        }
        String scheme = source.substring(0, schemaIndex);
        int length = source.length();
        int index = schemaIndex + 3;

        // host：IPv6 字面量形如 [::1]，其余一直取到 ':' '/' '?' '#' '@' 为止
        String host;
        if (index < length && source.charAt(index) == '[') {
            int closeIndex = source.indexOf(']', index);
            if (closeIndex < 0) {
                throw new IllegalArgumentException("invalid url:" + url);
            }
            host = source.substring(index + 1, closeIndex);
            index = closeIndex + 1;
        } else {
            int hostEnd = index;
            while (hostEnd < length && isHostChar(source.charAt(hostEnd))) {
                hostEnd++;
            }
            host = source.substring(index, hostEnd);
            index = hostEnd;
        }
        if (host.isEmpty()) {
            throw new IllegalArgumentException("invalid url:" + url);
        }

        // port：仅当冒号后紧跟数字时才成立，避免把路径里的 ':' 当成端口
        int port = -1;
        if (index < length && source.charAt(index) == ':') {
            int portStart = ++index;
            while (index < length && isDigit(source.charAt(index))) {
                index++;
            }
            if (index == portStart) {
                throw new IllegalArgumentException("invalid url:" + url);
            }
            port = FeatUtils.toInt(source.substring(portStart, index), -1);
            if (port <= 0 || port > 65535) {
                throw new IllegalArgumentException("invalid url:" + url);
            }
        }
        if (index < length && !isPathSeparator(source.charAt(index))) {
            throw new IllegalArgumentException("invalid url:" + url);
        }

        // path：fragment 不参与请求目标
        int fragmentIndex = source.indexOf('#', index);
        int pathEnd = fragmentIndex < 0 ? length : fragmentIndex;
        String path;
        if (index >= pathEnd) {
            path = "/";
        } else if (source.charAt(index) == '/') {
            path = source.substring(index, pathEnd);
        } else if (source.charAt(index) == '?') {
            path = "/" + source.substring(index, pathEnd);
        } else {
            path = "/";
        }
        if (port < 0) {
            port = defaultPort(scheme);
            if (port < 0) {
                throw new IllegalArgumentException("unsupported scheme [" + scheme + "] in url:" + url);
            }
        }
        return new HttpUrl(scheme, host, port, path);
    }

    private static boolean isHostChar(char c) {
        return c != ':' && c != '/' && c != '?' && c != '#' && c != '@' && c != '[' && c != ']';
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private static boolean isPathSeparator(char c) {
        return c == '/' || c == '?' || c == '#';
    }

    /**
     * 按协议推导缺省端口
     */
    private static int defaultPort(String scheme) {
        if (FeatUtils.SCHEMA_HTTP.equals(scheme) || SCHEMA_WS.equals(scheme)) {
            return 80;
        }
        if (FeatUtils.SCHEMA_HTTPS.equals(scheme) || SCHEMA_WSS.equals(scheme)) {
            return 443;
        }
        return -1;
    }

    public String getScheme() {
        return scheme;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    /**
     * 请求目标：path + query，恒以 {@code /} 开头
     */
    public String getPath() {
        return path;
    }

    /**
     * 是否加密连接（https / wss）
     */
    public boolean isSecure() {
        return FeatUtils.SCHEMA_HTTPS.equals(scheme) || SCHEMA_WSS.equals(scheme);
    }

    /**
     * Host 请求头的值。
     * <p>
     * 端口为协议缺省值（http/ws 的 80、https/wss 的 443）时省略，与 curl、OkHttp 等主流客户端保持一致；
     * 非缺省端口必须带上。IPv6 字面量会补回方括号。
     */
    public String getHostHeader() {
        return formatHost(port == defaultPort(scheme) ? -1 : port);
    }

    private String formatHost(int port) {
        String formattedHost = host.indexOf(':') >= 0 ? "[" + host + "]" : host;
        return port > 0 ? formattedHost + ":" + port : formattedHost;
    }

    @Override
    public String toString() {
        return scheme + "://" + getHostHeader() + path;
    }
}
