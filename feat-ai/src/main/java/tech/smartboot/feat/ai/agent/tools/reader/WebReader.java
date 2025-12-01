/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.agent.tools.reader;

import tech.smartboot.feat.core.client.HttpClient;
import tech.smartboot.feat.core.client.HttpGet;
import tech.smartboot.feat.core.client.HttpResponse;
import tech.smartboot.feat.core.common.HeaderName;
import tech.smartboot.feat.core.common.HttpStatus;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * 搜索器抽象基类
 * <p>
 * 该类定义了网络搜索功能的基础框架，支持不同搜索引擎的实现。
 * 通过模板方法模式，具体的搜索引擎实现只需要关注请求的URL和结果解析逻辑，
 * 而通用的HTTP请求处理、错误处理等由基类统一处理。
 * </p>
 *
 * @author 三刀
 * @version v1.0 11/26/25
 */
public class WebReader {

    /**
     * 执行搜索操作的通用方法
     * <p>
     * 根据URL自动选择合适的搜索引擎实现，并执行搜索请求。
     * 支持自定义HTTP GET请求参数。
     * </p>
     *
     * @param url      搜索URL
     * @param consumer 自定义HTTP GET请求的回调函数
     * @return 搜索结果的Markdown格式字符串
     */
    public static String search(String url, Consumer<HttpGet> consumer) {
        WebReader searcher;
        if (url.startsWith(BaiduReader.BASE_URL)) {
            searcher = new BaiduReader();
        } else if (url.startsWith(BingReader.BASE_URL)) {
            searcher = new BingReader();
        } else if (url.startsWith(OsChinaNewsReader.BASE_URL)) {
            searcher = new OsChinaNewsReader();
        } else {
            searcher = new WebReader();
        }
        HttpClient httpClient = new HttpClient(url);
        httpClient.options().debug(false);
        try {
            HttpGet httpGet = httpClient.get();
            if (consumer != null) {
                consumer.accept(httpGet);
            }
            httpGet.header(header -> {
                searcher.baseHeader().forEach(header::set);
            });
            searcher.initRequest(httpGet);
            HttpResponse response = httpGet.submit().get(5, TimeUnit.SECONDS);
            if (response.statusCode() != HttpStatus.OK.value()) {
                return "请求失败.";
            } else {
                return searcher.toMarkdown(response.body());
            }
        } catch (Throwable e) {
            e.printStackTrace();
            return "请求失败.";
        }
    }

    /**
     * 执行搜索操作的简化方法
     * <p>
     * 执行指定URL的搜索请求，不包含自定义参数。
     * </p>
     *
     * @param url 搜索URL
     * @return 搜索结果的Markdown格式字符串
     */
    public static String search(String url) {
        return search(url, null);
    }

    /**
     * 初始化HTTP请求
     * <p>
     * 子类可以重写此方法以添加特定搜索引擎所需的请求头或其他配置。
     * </p>
     *
     * @param httpGet HTTP GET请求对象
     */
    protected void initRequest(HttpGet httpGet) {
    }

    /**
     * 将HTML内容转换为Markdown格式
     * <p>
     * 抽象方法，由具体搜索引擎实现，负责将搜索引擎返回的HTML内容
     * 解析并转换为适合AI处理的Markdown格式文本。
     * </p>
     *
     * @param html HTML格式的搜索结果
     * @return Markdown格式的搜索结果
     */
    protected String toMarkdown(String html) {
        if (html == null || html.isEmpty()) {
            return "";
        }

        // Remove script and style tags
        html = html.replaceAll("(?is)<head[^>]*>.*?</head>", "");
        html = html.replaceAll("(?is)<script[^>]*>.*?</script>", "");
        html = html.replaceAll("(?is)<style[^>]*>.*?</style>", "");
        html = html.replaceAll("(?is)<footer[^>]*>.*?</footer>", "");

        // Remove HTML comments
        html = html.replaceAll("(?is)<!--.*?-->", "");

        // Handle headings
        html = html.replaceAll("(?i)</?h1>", "\n# ");
        html = html.replaceAll("(?i)</?h2>", "\n## ");
        html = html.replaceAll("(?i)</?h3>", "\n### ");
        html = html.replaceAll("(?i)</?h4>", "\n#### ");
        html = html.replaceAll("(?i)</?h5>", "\n##### ");
        html = html.replaceAll("(?i)</?h6>", "\n###### ");
        //合并空白字符
        html = html.replaceAll("\\s+", " ");

        // Handle paragraphs
        html = html.replaceAll("(?i)<p[^>]*>", "\n\n");
        html = html.replaceAll("(?i)</p>", "\n");

        html = html.replaceAll("(?i)<div[^>]*>", "\n\n");
        html = html.replaceAll("(?i)</div>", "\n");


        // Handle line breaks
        html = html.replaceAll("(?i)<br[^>]*/?>", "\n");

        // Handle bold/strong
        html = html.replaceAll("(?i)<strong[^>]*>(.*?)</strong>", "**$1**");
        html = html.replaceAll("(?i)<b[^>]*>(.*?)</b>", "**$1**");

        // Handle italic/em
        html = html.replaceAll("(?i)<em[^>]*>(.*?)</em>", "*$1*");
        html = html.replaceAll("(?i)<i[^>]*>(.*?)</i>", "*$1*");

        // Handle links
        html = html.replaceAll("(?i)<a[^>]*href=[\"']([^\"']*)[\"'][^>]*>(.*?)</a>", "[$2]($1)");

        // Handle images
        html = html.replaceAll("(?i)<img[^>]*src=[\"']([^\"']*)[\"'][^>]*alt=[\"']([^\"']*)[\"'][^>]*/?>", "![$2]($1)");
        html = html.replaceAll("(?i)<img[^>]*alt=[\"']([^\"']*)[\"'][^>]*src=[\"']([^\"']*)[\"'][^>]*/?>", "![$1]($2)");

        // Handle lists
        html = html.replaceAll("(?i)<ul[^>]*>", "\n");
        html = html.replaceAll("(?i)</ul>", "\n");
        html = html.replaceAll("(?i)<ol[^>]*>", "\n");
        html = html.replaceAll("(?i)</ol>", "\n");
        html = html.replaceAll("(?i)<li[^>]*>", "\n- ");
        html = html.replaceAll("(?i)</li>", "\n");

        // Handle code blocks
        html = html.replaceAll("(?i)<pre[^>]*>", "\n```\n");
        html = html.replaceAll("(?i)</pre>", "\n```\n");
        html = html.replaceAll("(?i)<code[^>]*>", "`");
        html = html.replaceAll("(?i)</code>", "`");

        // Handle blockquotes
        html = html.replaceAll("(?i)<blockquote[^>]*>", "\n> ");
        html = html.replaceAll("(?i)</blockquote>", "\n");

        // Handle horizontal rules
        html = html.replaceAll("(?i)<hr[^>]*/?>", "\n---\n");

        // Remove remaining HTML tags
        html = html.replaceAll("(?i)<[^>]*>", "");

        // Clean up extra whitespace
        html = html.replaceAll("\n\\s+\n", "\n\n");
        html = html.replaceAll("\n{3,}", "\n\n");
        html = html.trim();

        return html;
    }

    /**
     * 获取基础HTTP请求头
     * <p>
     * 定义通用的浏览器请求头，使搜索请求看起来像是来自真实用户的浏览器，
     * 避免被搜索引擎识别为机器人请求。
     * </p>
     *
     * @return 包含基础请求头的Map
     */
    private Map<String, String> baseHeader() {
        Map<String, String> headers = new HashMap<>();
        headers.put(HeaderName.ACCEPT.getName(), "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");
        headers.put(HeaderName.ACCEPT_ENCODING.getName(), "gzip, deflate, zstd, dcb, dcz");
        headers.put(HeaderName.ACCEPT_LANGUAGE.getName(), "zh-CN,zh;q=0.9");
        headers.put(HeaderName.USER_AGENT.getName(), "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36");
        headers.put(HeaderName.CACHE_CONTROL.getName(), "max-age=0");
        headers.put("Sec-Ch-Ua", "\"Chromium\";v=\"142\", \"Google Chrome\";v=\"142\", \"Not_A Brand\";v=\"99\"");
        headers.put("Sec-Ch-Ua-Arch", "");
        headers.put("Sec-Ch-Ua-Bitness", "\"64\"");
        headers.put("Sec-Ch-Ua-Full-Version", "\"142.0.7444.163\"");
        headers.put("Sec-Ch-Ua-Full-Version-List", "\"Chromium\";v=\"142.0.7444.163\", \"Google Chrome\";v=\"142.0.7444.163\", \"Not_A Brand\";v=\"99.0.0.0\"");
        headers.put("Sec-Ch-Ua-Mobile", "?0");
        headers.put("Sec-Ch-Ua-Platform", "\"macOS\"");
        headers.put("Sec-Fetch-Dest", "document");
        headers.put("Sec-Fetch-Mode", "navigate");
        headers.put("Sec-Fetch-Site", "same-origin");
        headers.put("Sec-Fetch-User", "?1");
        headers.put("Prority", "u=0, i");
        headers.put("Upgrade-Insecure-Requests", "1");
        return headers;
    }
}