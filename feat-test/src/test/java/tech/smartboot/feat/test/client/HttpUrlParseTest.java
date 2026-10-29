/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the Apache-2.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.test.client;

import org.junit.Test;
import tech.smartboot.feat.core.common.HttpUrl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * {@link HttpUrl} 解析用例，纯内存断言，不依赖网络。
 *
 * @author 三刀 zhengjunweimail@163.com
 */
public class HttpUrlParseTest {

    private static void assertUrl(String url, String scheme, String host, int port, String path) {
        HttpUrl httpUrl = HttpUrl.parse(url);
        assertEquals(url + " -> scheme", scheme, httpUrl.getScheme());
        assertEquals(url + " -> host", host, httpUrl.getHost());
        assertEquals(url + " -> port", port, httpUrl.getPort());
        assertEquals(url + " -> path", path, httpUrl.getPath());
    }

    @Test
    public void withoutPath() {
        assertUrl("http://example.com", "http", "example.com", 80, "/");
        assertUrl("http://example.com/", "http", "example.com", 80, "/");
        assertUrl("https://example.com", "https", "example.com", 443, "/");
    }

    /**
     * 无路径时，查询串不能混入 host，且请求目标需要补上 '/'
     */
    @Test
    public void queryWithoutPath() {
        assertUrl("http://example.com?x=1", "http", "example.com", 80, "/?x=1");
        assertUrl("https://example.com?x=1", "https", "example.com", 443, "/?x=1");
        assertUrl("http://example.com:8080?x=1", "http", "example.com", 8080, "/?x=1");
        // 查询串里同时出现 ':' 与 '/'，不能干扰端口与路径的判定
        assertUrl("http://example.com:8080?next=/a:b", "http", "example.com", 8080, "/?next=/a:b");
        assertUrl("http://example.com?x=a:b", "http", "example.com", 80, "/?x=a:b");
        assertUrl("http://example.com?x=/a:b", "http", "example.com", 80, "/?x=/a:b");
    }

    @Test
    public void withPath() {
        assertUrl("http://example.com/path", "http", "example.com", 80, "/path");
        assertUrl("http://example.com/path?x=1", "http", "example.com", 80, "/path?x=1");
        assertUrl("http://example.com:8080/path?x=1", "http", "example.com", 8080, "/path?x=1");
        // 路径里的 ':' 不能被当成端口分隔符
        assertUrl("https://r.jina.ai/https://smartboot.tech", "https", "r.jina.ai", 443, "/https://smartboot.tech");
        assertUrl("https://r.jina.ai/https://smartboot.tech?a=b:c", "https", "r.jina.ai", 443, "/https://smartboot.tech?a=b:c");
    }

    /**
     * fragment 不参与请求目标
     */
    @Test
    public void withoutFragment() {
        assertUrl("http://example.com/path?x=1#frag", "http", "example.com", 80, "/path?x=1");
        assertUrl("http://example.com#frag", "http", "example.com", 80, "/");
        assertUrl("http://example.com?x=1#frag", "http", "example.com", 80, "/?x=1");
    }

    @Test
    public void ipv6Literal() {
        assertUrl("http://[::1]:8080/path", "http", "::1", 8080, "/path");
        assertUrl("http://[::1]?x=1", "http", "::1", 80, "/?x=1");
        assertEquals("[::1]:8080", HttpUrl.parse("http://[::1]:8080/path").getHostHeader());
    }

    @Test
    public void webSocketScheme() {
        assertUrl("ws://example.com/socket", "ws", "example.com", 80, "/socket");
        assertUrl("wss://example.com?token=1", "wss", "example.com", 443, "/?token=1");
        assertFalse(HttpUrl.parse("ws://example.com/socket").isSecure());
        assertTrue(HttpUrl.parse("wss://example.com/socket").isSecure());
    }

    @Test
    public void hostHeader() {
        // 协议缺省端口不写入 Host 头
        assertEquals("example.com", HttpUrl.parse("http://example.com").getHostHeader());
        assertEquals("example.com", HttpUrl.parse("http://example.com:80/p").getHostHeader());
        assertEquals("example.com", HttpUrl.parse("https://example.com").getHostHeader());
        assertEquals("example.com", HttpUrl.parse("https://example.com:443").getHostHeader());
        assertEquals("example.com", HttpUrl.parse("ws://example.com/socket").getHostHeader());
        assertEquals("example.com", HttpUrl.parse("wss://example.com/socket").getHostHeader());
        // 非缺省端口必须写入
        assertEquals("example.com:8080", HttpUrl.parse("http://example.com:8080/p").getHostHeader());
        assertEquals("example.com:443", HttpUrl.parse("http://example.com:443/p").getHostHeader());
        assertEquals("example.com:80", HttpUrl.parse("https://example.com:80/p").getHostHeader());
    }

    @Test
    public void invalidUrl() {
        assertThrows(IllegalArgumentException.class, () -> HttpUrl.parse(null));
        assertThrows(IllegalArgumentException.class, () -> HttpUrl.parse("example.com/path"));
        assertThrows(IllegalArgumentException.class, () -> HttpUrl.parse("http://"));
        assertThrows(IllegalArgumentException.class, () -> HttpUrl.parse("http://example.com:"));
        assertThrows(IllegalArgumentException.class, () -> HttpUrl.parse("http://example.com:abc"));
        assertThrows(IllegalArgumentException.class, () -> HttpUrl.parse("http://example.com:70000"));
        assertThrows(IllegalArgumentException.class, () -> HttpUrl.parse("ftp://example.com"));
        // userinfo 暂不支持，需明确报错而不是解析出错误的 host
        assertThrows(IllegalArgumentException.class, () -> HttpUrl.parse("http://user:pass@example.com/path"));
    }
}
