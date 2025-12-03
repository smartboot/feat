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
import tech.smartboot.feat.core.common.FeatUtils;
import tech.smartboot.feat.core.common.HeaderName;
import tech.smartboot.feat.core.common.HttpStatus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    public static String read(String url, Consumer<HttpGet> consumer) {
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
    public static String read(String url) {
        return read(url, null);
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

        // 提取<head>标签中的信息
        int index = html.indexOf("</head>");
        String headMarkdown = "";
        if (index > 0) {
            headMarkdown = extractHeadInformation(html.substring(0, index + 7));
        }

        String body = html.substring(index + 7).replaceAll("\\s+", " ");

        // Remove script and style tags
        body = body.replaceAll("(?is)<script[^>]*>.*?</script>", "");
        body = body.replaceAll("(?is)<style[^>]*>.*?</style>", "");
        //移除display:none
        // 优化正则表达式以更准确地匹配具有display:none样式的元素
        body = body.replaceAll("(?is)<[^>]*style\\s*=\\s*[\"'][^\"']*display\\s*:\\s*none[^\"']*[\"'][^>]*>.*?</[^>]*>", "");
//        body = body.replaceAll("(?is)<footer[^>]*>.*?</footer>", "");

        // Remove HTML comments
        body = body.replaceAll("(?is)<!--.*?-->", "");

        //循环移除空标签
        String preBody = body;
        while (true) {
            body = body.replaceAll("(?is)<[^/>]*>\\s*</[^>]*>", "");
            if (body.equals(preBody)) {
                break;
            } else {
                preBody = body;
            }
        }

        // Handle headings
        body = body.replaceAll("(?is)<h1[^>]*>([^>]*)</h1>", "\n# $1");
        body = body.replaceAll("(?is)<h2[^>]*>([^>]*)</h2>", "\n## $1");
        body = body.replaceAll("(?is)<h3[^>]*>([^>]*)</h3>", "\n#### $1");
        body = body.replaceAll("(?is)<h4[^>]*>([^>]*)</h4>", "\n##### $1");
        body = body.replaceAll("(?is)<h5[^>]*>([^>]*)</h5>", "\n###### $1");
        body = body.replaceAll("(?is)<h6[^>]*>([^>]*)</h6>", "\n###### $1");

        // Handle paragraphs
        body = body.replaceAll("(?i)<p[^>]*>", "\n\n");
        body = body.replaceAll("(?i)</p>", "\n");

        body = body.replaceAll("(?i)<div[^>]*>", "\n\n");
        body = body.replaceAll("(?i)</div>", "\n");


        // Handle line breaks
        body = body.replaceAll("(?i)<br[^>]*/?>", "\n");

        // Handle bold/strong
        body = body.replaceAll("(?i)<strong[^>]*>(.*?)</strong>", "**$1**");
        body = body.replaceAll("(?i)<b[^>]*>(.*?)</b>", "**$1**");

        // Handle italic/em
        body = body.replaceAll("(?i)<em[^>]*>(.*?)</em>", "*$1*");
        body = body.replaceAll("(?i)<i[^>]*>(.*?)</i>", "*$1*");

        // Handle links
        body = body.replaceAll("(?i)<a[^>]*href=[\"']([^\"']*)[\"'][^>]*>(.*?)</a>", "[$2]($1)");

        // Handle images
        body = body.replaceAll("(?i)<img[^>]*src=[\"']([^\"']*)[\"'][^>]*alt=[\"']([^\"']*)[\"'][^>]*/?>", "![$2]($1)");
        body = body.replaceAll("(?i)<img[^>]*alt=[\"']([^\"']*)[\"'][^>]*src=[\"']([^\"']*)[\"'][^>]*/?>", "![$1]($2)");

        // Handle lists
        body = body.replaceAll("(?i)<ul[^>]*>", "\n");
        body = body.replaceAll("(?i)</ul>", "\n");
        body = body.replaceAll("(?i)<ol[^>]*>", "\n");
        body = body.replaceAll("(?i)</ol>", "\n");
        body = body.replaceAll("(?i)<li[^>]*>", "\n- ");
        body = body.replaceAll("(?i)</li>", "\n");

        // Handle code blocks
        body = body.replaceAll("(?i)<pre[^>]*>", "\n```\n");
        body = body.replaceAll("(?i)</pre>", "\n```\n");
        body = body.replaceAll("(?i)<code[^>]*>", "`");
        body = body.replaceAll("(?i)</code>", "`");

        // Handle blockquotes
        body = body.replaceAll("(?i)<blockquote[^>]*>", "\n> ");
        body = body.replaceAll("(?i)</blockquote>", "\n");

        // Handle horizontal rules
        body = body.replaceAll("(?i)<hr[^>]*/?>", "\n---\n");

        // Remove remaining HTML tags
        body = body.replaceAll("(?i)<[^>]*>", "");

        // Clean up extra whitespace
        body = body.replaceAll("\n\\s+\n", "\n\n");
        body = body.replaceAll("\n{3,}", "\n\n");
        body = body.trim();

        StringBuilder markdown = new StringBuilder();
        markdown.append("# 网站基础信息\r\n").append(headMarkdown);
        markdown.append("\r\n# 网站Body\r\n");
        markdown.append(body);
        return markdown.toString();
    }

    /**
     * 提取HTML head标签中的信息
     * <p>
     * 从HTML的head部分提取标题、元数据和链接信息，包括：
     * - 页面标题(title)
     * - 字符编码(charset)
     * - 页面描述(description)
     * - 页面关键词(keywords)
     * - 样式表链接(stylesheet)
     * - 网站图标链接(favicon)
     * - RSS订阅链接
     * </p>
     *
     * @param html 完整的HTML内容
     * @return 替换<head>部分后的HTML内容
     */
    private String extractHeadInformation(String html) {
        String headPattern = "(?is)<head[^>]*>.*?</head>";
        String titlePattern = "(?i)<title[^>]*>(.*?)</title>";
        List<String> rssList = new ArrayList<>();
        String title = "";
        String charset = "";
        String description = "";
        String keywords = "";
        // 查找<head>块
        java.util.regex.Pattern headRegex = java.util.regex.Pattern.compile(headPattern);
        java.util.regex.Matcher headMatcher = headRegex.matcher(html);

        if (headMatcher.find()) {
            String headContent = headMatcher.group();

            // 从<head>中提取<title>
            java.util.regex.Pattern titleRegex = java.util.regex.Pattern.compile(titlePattern);
            java.util.regex.Matcher titleMatcher = titleRegex.matcher(headContent);

            if (titleMatcher.find()) {
                title = titleMatcher.group(1).trim();
                // 移除换行符并添加到替换内容中
                title = title.replaceAll("[\\r\\n]+", " ");
            }

            // 提取<meta>标签信息
            java.util.regex.Pattern metaPattern = java.util.regex.Pattern.compile("(?i)<meta\\s+([^>]*?)>");
            java.util.regex.Matcher metaMatcher = metaPattern.matcher(headContent);

            while (metaMatcher.find()) {
                String metaTag = metaMatcher.group(1);

                // 提取charset信息
                if (metaTag.contains("charset")) {
                    java.util.regex.Pattern charsetPattern = java.util.regex.Pattern.compile("(?i)charset=['\"]?([^'\"\\s>]+)");
                    java.util.regex.Matcher charsetMatcher = charsetPattern.matcher(metaTag);
                    if (charsetMatcher.find()) {
                        charset = charsetMatcher.group(1).trim();
                    }
                }

                // 提取description信息
                if (metaTag.contains("name=\"description\"") || metaTag.contains("name='description'")) {
                    java.util.regex.Pattern descPattern = java.util.regex.Pattern.compile("(?i)content=['\"]([^\"']*)['\"]");
                    java.util.regex.Matcher descMatcher = descPattern.matcher(metaTag);
                    if (descMatcher.find()) {
                        description = descMatcher.group(1).trim();
                        description = description.replaceAll("[\\r\\n]+", " ");
                    }
                }

                // 提取keywords信息
                if (metaTag.contains("name=\"keywords\"") || metaTag.contains("name='keywords'")) {
                    java.util.regex.Pattern keywordsPattern = java.util.regex.Pattern.compile("(?i)content=['\"]([^\"']*)['\"]");
                    java.util.regex.Matcher keywordsMatcher = keywordsPattern.matcher(metaTag);
                    if (keywordsMatcher.find()) {
                        keywords = keywordsMatcher.group(1).trim();
                        keywords = keywords.replaceAll("[\\r\\n]+", " ");
                    }
                }
            }

            // 提取<link>标签信息
            java.util.regex.Pattern linkPattern = java.util.regex.Pattern.compile("(?i)<link\\s+([^>]*?)>");
            java.util.regex.Matcher linkMatcher = linkPattern.matcher(headContent);

            while (linkMatcher.find()) {
                String linkTag = linkMatcher.group(1);

                // 提取RSS链接
                if (linkTag.contains("type=\"application/rss+xml\"") || linkTag.contains("type='application/rss+xml'")) {
                    java.util.regex.Pattern hrefPattern = java.util.regex.Pattern.compile("(?i)href=['\"]([^\"']*)['\"]");
                    java.util.regex.Matcher hrefMatcher = hrefPattern.matcher(linkTag);

                    java.util.regex.Pattern titlePatternLink = java.util.regex.Pattern.compile("(?i)title=['\"]([^\"']*)['\"]");
                    java.util.regex.Matcher titleMatcherLink = titlePatternLink.matcher(linkTag);

                    String rssTitle = "";
                    String rssUrl = "";
                    if (hrefMatcher.find()) {
                        rssUrl = hrefMatcher.group(1);
                    }

                    if (titleMatcherLink.find()) {
                        rssTitle = titleMatcherLink.group(1);
                    } else {
                        rssTitle = rssUrl;
                    }
                    if (FeatUtils.isNotBlank(rssUrl)) {
                        rssList.add('[' + rssTitle + "](" + rssUrl + ")");
                    }
                }
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append("**标题**: ").append(title).append("\r\n");
        sb.append("**编码**: ").append(charset).append("\r\n");
        sb.append("**描述**: ").append(description).append("\r\n");
        sb.append("**关键词**: ").append(keywords).append("\r\n");
        sb.append("**RSS订阅**: ");
        rssList.forEach(rss -> sb.append("\n\t- ").append(rss));
        sb.append("\r\n");

        return sb.toString();
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