/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the Apache-2.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.agent.tools.reader;

import tech.smartboot.feat.core.client.HttpClient;
import tech.smartboot.feat.core.client.HttpGet;
import tech.smartboot.feat.core.common.HeaderName;
import tech.smartboot.feat.core.common.HttpStatus;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 网页内容阅读器
 * <p>
 * 参考 r.jina.ai 的实现原理，提供高质量的网页内容提取功能：
 * 1. 使用 Readability 风格算法提取主要内容区域
 * 2. 过滤广告、导航、脚本等干扰内容
 * 3. 输出 LLM 友好的 Markdown 格式
 * </p>
 *
 * @author 三刀
 * @version v1.0 11/26/25
 */
public class WebReader {

    /**
     * 执行网页读取操作的通用方法（支持格式指定）
     * <p>
     * 根据URL自动选择合适的阅读器实现，并执行请求。
     * 支持自定义HTTP GET请求参数和响应格式。
     * </p>
     *
     * @param url      目标URL
     * @param consumer 自定义HTTP GET请求的回调函数
     * @return 读取结果字符串
     */
    public static CompletableFuture<String> read(String url, Consumer<HttpGet> consumer) {
        WebReader reader;
        if (url.startsWith(BaiduReader.BASE_URL)) {
            reader = new BaiduReader();
        } else if (url.startsWith(BingReader.BASE_URL)) {
            reader = new BingReader();
        } else {
            reader = new WebReader();
        }
        return reader.doRead(url, consumer);
    }

    /**
     * 执行实际的网页读取操作
     *
     * @param url      目标URL
     * @param consumer 自定义HTTP GET请求的回调函数
     * @return 读取结果字符串
     */
    protected CompletableFuture<String> doRead(String url, Consumer<HttpGet> consumer) {
        HttpClient httpClient = new HttpClient(url);
        httpClient.options().debug(false).idleTimeout(5000);
        HttpGet httpGet = httpClient.get();
        if (consumer != null) {
            consumer.accept(httpGet);
        }
        httpGet.header(header -> {
            simulatorDeviceHeader().forEach(header::set);
        });
        initRequest(httpGet);
        return httpGet.submit().thenApply(response -> {
            if (response.statusCode() != HttpStatus.OK.value()) {
                return "请求失败.";
            } else {
                String body = response.body();
                if (body == null || body.isEmpty()) {
                    return "请求失败：返回内容为空.";
                }
                return toMarkdown(body, url);
            }
        }).exceptionally(throwable -> {
            throwable.printStackTrace();
            return "请求失败: " + throwable.getMessage();
        });
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
    public static CompletableFuture<String> read(String url) {
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
     * 参考 r.jina.ai 的实现原理，使用 Readability 风格的算法提取主要内容，
     * 过滤广告、导航、脚本等干扰内容，生成 LLM 友好的 Markdown。
     * </p>
     *
     * @param html HTML格式的网页内容
     * @param url  原始URL，用于构建相对链接
     * @return Markdown格式的网页内容
     */
    protected String toMarkdown(String html, String url) {
        if (html == null || html.isEmpty()) {
            return "";
        }

        // 提取页面元信息
        PageMetadata metadata = extractPageMetadata(html, url);

        // 提取主要内容（Readability 风格）
//        String mainContent = extractMainContent(html);

        // 转换为 Markdown
        String markdownContent = htmlToMarkdown(html, url);

        // 组装最终输出（参考 r.jina.ai 格式）
        StringBuilder result = new StringBuilder();
        result.append("Title: ").append(metadata.title).append("\n\n");
        result.append("URL Source: ").append(metadata.sourceUrl).append("\n\n");
        if (!metadata.publishedTime.isEmpty()) {
            result.append("Published Time: ").append(metadata.publishedTime).append("\n\n");
        }
        result.append("Markdown Content:\n");
        result.append(markdownContent);

        return result.toString();
    }


    /**
     * 页面元数据结构
     */
    protected static class PageMetadata {
        String title = "";
        String sourceUrl = "";
        String publishedTime = "";
        String description = "";
        String author = "";
    }

    /**
     * 提取页面元数据
     *
     * @param html 原始 HTML
     * @param url  原始 URL
     * @return 页面元数据
     */
    protected PageMetadata extractPageMetadata(String html, String url) {
        PageMetadata metadata = new PageMetadata();
        metadata.sourceUrl = url != null ? url : "";

        // 提取标题
        Pattern titlePattern = Pattern.compile("(?i)<title[^>]*>(.*?)</title>");
        Matcher titleMatcher = titlePattern.matcher(html);
        if (titleMatcher.find()) {
            metadata.title = titleMatcher.group(1).trim().replaceAll("[\\r\\n]+", " ");
        }

        // 提取 og:title（优先使用）
        Pattern ogTitlePattern = Pattern.compile(
                "(?i)<meta[^>]*property=[\"']og:title[\"'][^>]*content=[\"']([^\"']*)[\"']");
        Matcher ogTitleMatcher = ogTitlePattern.matcher(html);
        if (ogTitleMatcher.find()) {
            metadata.title = ogTitleMatcher.group(1).trim();
        }

        // 提取描述
        Pattern descPattern = Pattern.compile(
                "(?i)<meta[^>]*name=[\"']description[\"'][^>]*content=[\"']([^\"']*)[\"']");
        Matcher descMatcher = descPattern.matcher(html);
        if (descMatcher.find()) {
            metadata.description = descMatcher.group(1).trim();
        }

        // 提取作者
        Pattern authorPattern = Pattern.compile(
                "(?i)<meta[^>]*name=[\"']author[\"'][^>]*content=[\"']([^\"']*)[\"']");
        Matcher authorMatcher = authorPattern.matcher(html);
        if (authorMatcher.find()) {
            metadata.author = authorMatcher.group(1).trim();
        }

        // 提取发布时间（常见格式）
        String[] timePatterns = {
                "(?i)<meta[^>]*name=[\"']publish-date[\"'][^>]*content=[\"']([^\"']*)[\"']",
                "(?i)<meta[^>]*name=[\"']published_date[\"'][^>]*content=[\"']([^\"']*)[\"']",
                "(?i)<meta[^>]*name=[\"']article:published_time[\"'][^>]*content=[\"']([^\"']*)[\"']",
                "(?i)<time[^>]*datetime=[\"']([^\"']*)[\"']"
        };
        for (String pattern : timePatterns) {
            Pattern timeRegex = Pattern.compile(pattern);
            Matcher timeMatcher = timeRegex.matcher(html);
            if (timeMatcher.find()) {
                metadata.publishedTime = timeMatcher.group(1).trim();
                break;
            }
        }

        return metadata;
    }

    /**
     * 提取主要内容（Readability 风格算法）
     * <p>
     * 基于启发式算法识别页面主要内容区域，过滤导航、广告、侧边栏等。
     * </p>
     *
     * @param html 原始 HTML
     * @return 主要内容区域的 HTML
     */
    protected String extractMainContent(String html) {
        if (html == null || html.isEmpty()) {
            return "";
        }

        // 首先尝试找到 article 标签
        Pattern articlePattern = Pattern.compile(
                "(?is)<article[^>]*>(.*?)</article>", Pattern.DOTALL);
        Matcher articleMatcher = articlePattern.matcher(html);
        if (articleMatcher.find()) {
            String article = articleMatcher.group(1);
            // 确保 article 内容足够长
            if (article.length() > 200) {
                return article;
            }
        }

        // 尝试找到 main 标签
        Pattern mainPattern = Pattern.compile(
                "(?is)<main[^>]*>(.*?)</main>", Pattern.DOTALL);
        Matcher mainMatcher = mainPattern.matcher(html);
        if (mainMatcher.find()) {
            String main = mainMatcher.group(1);
            if (main.length() > 200) {
                return main;
            }
        }

        // 提取 body
        int bodyStart = html.indexOf("<body");
        int bodyEnd = html.lastIndexOf("</body>");
        if (bodyStart < 0 || bodyEnd < 0 || bodyEnd <= bodyStart) {
            bodyStart = html.indexOf("</head>");
            if (bodyStart < 0) {
                bodyStart = 0;
            } else {
                bodyStart += 7;
            }
            bodyEnd = html.length();
        } else {
            // 找到 body 标签的结束位置
            bodyStart = html.indexOf(">", bodyStart) + 1;
        }

        String body = html.substring(bodyStart, bodyEnd);

        // 使用启发式评分找到最佳内容块
        return findBestContentBlock(body);
    }

    /**
     * 使用启发式算法找到最佳内容块
     *
     * @param body body 部分的 HTML
     * @return 最佳内容块
     */
    protected String findBestContentBlock(String body) {
        // 先移除 script、style、nav 等明显非内容标签
        String cleaned = body.replaceAll("(?is)<script[^>]*>.*?</script>", "");
        cleaned = cleaned.replaceAll("(?is)<style[^>]*>.*?</style>", "");
        cleaned = cleaned.replaceAll("(?is)<nav[^>]*>.*?</nav>", "");
        cleaned = cleaned.replaceAll("(?is)<header[^>]*>.*?</header>", "");
        cleaned = cleaned.replaceAll("(?is)<footer[^>]*>.*?</footer>", "");
        cleaned = cleaned.replaceAll("(?is)<aside[^>]*>.*?</aside>", "");
        cleaned = cleaned.replaceAll("(?is)<!--.*?-->", "");

        // 查找可能的 content 区域
        // 优先查找 id 或 class 包含 content、article、post 等关键字的 div
        String[] contentPatterns = {
                "(?is)<div[^>]*class=[\"'][^\"']*content[^\"']*[\"'][^>]*>(.*?)</div>",
                "(?is)<div[^>]*class=[\"'][^\"']*article[^\"']*[\"'][^>]*>(.*?)</div>",
                "(?is)<div[^>]*class=[\"'][^\"']*post[^\"']*[\"'][^>]*>(.*?)</div>",
                "(?is)<div[^>]*id=[\"'][^\"']*content[^\"']*[\"'][^>]*>(.*?)</div>",
                "(?is)<div[^>]*id=[\"'][^\"']*article[^\"']*[\"'][^>]*>(.*?)</div>",
                "(?is)<section[^>]*>(.*?)</section>"
        };

        String bestBlock = "";
        int bestScore = 0;

        for (String pattern : contentPatterns) {
            Pattern regex = Pattern.compile(pattern, Pattern.DOTALL);
            Matcher matcher = regex.matcher(cleaned);
            while (matcher.find()) {
                String block = matcher.group(1);
                int score = scoreContentBlock(block);
                if (score > bestScore) {
                    bestScore = score;
                    bestBlock = block;
                }
            }
        }

        // 如果没有找到足够好的内容块，返回清理后的 body
        if (bestBlock.isEmpty() || bestScore < 100) {
            return cleaned;
        }

        return bestBlock;
    }

    /**
     * 对内容块进行评分
     *
     * @param block HTML 内容块
     * @return 内容质量分数
     */
    protected int scoreContentBlock(String block) {
        if (block == null || block.isEmpty()) {
            return 0;
        }

        int score = 0;

        // 文本长度加分
        int textLength = block.replaceAll("(?i)<[^>]*>", "").length();
        score += textLength;

        // 段落数量加分
        Pattern pPattern = Pattern.compile("(?i)<p[^>]*>");
        Matcher pMatcher = pPattern.matcher(block);
        int pCount = 0;
        while (pMatcher.find()) {
            pCount++;
        }
        score += pCount * 100;

        // 链接密度减分（链接比例过高可能是导航）
        int linkCount = 0;
        Pattern aPattern = Pattern.compile("(?i)<a[^>]*>");
        Matcher aMatcher = aPattern.matcher(block);
        while (aMatcher.find()) {
            linkCount++;
        }
        if (textLength > 0 && linkCount > 0) {
            double linkDensity = (double) linkCount / (textLength / 50.0);
            if (linkDensity > 1.0) {
                score -= (int) (linkDensity * 100);
            }
        }

        // 包含负面关键词减分
        String[] negativeIndicators = {"comment", "sidebar", "widget", "advertisement", "ad-", "menu", "nav"};
        String lowerBlock = block.toLowerCase();
        for (String indicator : negativeIndicators) {
            if (lowerBlock.contains(indicator)) {
                score -= 50;
            }
        }

        // 正面关键词加分
        String[] positiveIndicators = {"content", "article", "post", "entry", "text"};
        for (String indicator : positiveIndicators) {
            if (lowerBlock.contains(indicator)) {
                score += 50;
            }
        }

        return score;
    }

    /**
     * 将 HTML 转换为 Markdown
     *
     * @param html 原始 HTML
     * @param url  原始 URL（用于处理相对链接）
     * @return Markdown 格式文本
     */
    protected String htmlToMarkdown(String html, String url) {
        if (html == null || html.isEmpty()) {
            return "";
        }

        String body = html;

        // 规范化空白字符
        body = body.replaceAll("\\s+", " ");

        // 移除 script 和 style
        body = body.replaceAll("(?is)<script[^>]*>.*?</script>", "");
        body = body.replaceAll("(?is)<style[^>]*>.*?</style>", "");

        // 移除 display:none 元素
        body = body.replaceAll("(?is)<[^>]*style\\s*=\\s*[\"'][^\"']*display\\s*:\\s*none[^\"']*[\"'][^>]*>.*?</[^>]*>", "");

        // 移除注释
        body = body.replaceAll("(?is)<!--.*?-->", "");

        // 移除空标签
        String prevBody;
        do {
            prevBody = body;
            body = body.replaceAll("(?is)<[^/>]*>\\s*</[^>]*>", "");
        } while (!body.equals(prevBody));

        // 转换标题
        body = body.replaceAll("(?is)<h1[^>]*>(.*?)</h1>", "\n# $1\n");
        body = body.replaceAll("(?is)<h2[^>]*>(.*?)</h2>", "\n## $1\n");
        body = body.replaceAll("(?is)<h3[^>]*>(.*?)</h3>", "\n### $1\n");
        body = body.replaceAll("(?is)<h4[^>]*>(.*?)</h4>", "\n#### $1\n");
        body = body.replaceAll("(?is)<h5[^>]*>(.*?)</h5>", "\n##### $1\n");
        body = body.replaceAll("(?is)<h6[^>]*>(.*?)</h6>", "\n###### $1\n");

        // 转换段落和块级元素
        body = body.replaceAll("(?i)<p[^>]*>", "\n\n");
        body = body.replaceAll("(?i)</p>", "\n");
        body = body.replaceAll("(?i)<div[^>]*>", "\n");
        body = body.replaceAll("(?i)</div>", "\n");
        body = body.replaceAll("(?i)<br[^>]*/?>", "\n");

        // 转换格式标签
        body = body.replaceAll("(?i)<strong[^>]*>(.*?)</strong>", "**$1**");
        body = body.replaceAll("(?i)<b[^>]*>(.*?)</b>", "**$1**");
        body = body.replaceAll("(?i)<em[^>]*>(.*?)</em>", "*$1*");
        body = body.replaceAll("(?i)<i[^>]*>(.*?)</i>", "*$1*");

        // 转换链接 - 处理相对链接
        body = resolveLinks(body, url);

        // 转换图片
        body = body.replaceAll("(?i)<img[^>]*src=[\"']([^\"']*)[\"'][^>]*alt=[\"']([^\"']*)[\"'][^>]*/?>", "![$2]($1)");
        body = body.replaceAll("(?i)<img[^>]*alt=[\"']([^\"']*)[\"'][^>]*src=[\"']([^\"']*)[\"'][^>]*/?>", "![$1]($2)");
        body = body.replaceAll("(?i)<img[^>]*src=[\"']([^\"']*)[\"'][^>]*/?>", "![Image]($1)");

        // 转换列表
        body = body.replaceAll("(?i)<ul[^>]*>", "\n");
        body = body.replaceAll("(?i)</ul>", "\n");
        body = body.replaceAll("(?i)<ol[^>]*>", "\n");
        body = body.replaceAll("(?i)</ol>", "\n");
        body = body.replaceAll("(?i)<li[^>]*>", "\n- ");
        body = body.replaceAll("(?i)</li>", "\n");

        // 转换代码块
        body = body.replaceAll("(?i)<pre[^>]*>", "\n```\n");
        body = body.replaceAll("(?i)</pre>", "\n```\n");
        body = body.replaceAll("(?i)<code[^>]*>", "`");
        body = body.replaceAll("(?i)</code>", "`");

        // 转换引用
        body = body.replaceAll("(?i)<blockquote[^>]*>", "\n> ");
        body = body.replaceAll("(?i)</blockquote>", "\n");

        // 转换水平线
        body = body.replaceAll("(?i)<hr[^>]*/?>", "\n---\n");

        // 移除剩余的 HTML 标签
        body = body.replaceAll("(?i)<[^>]*>", "");

        // 解码 HTML 实体
        body = body.replaceAll("<", "<");
        body = body.replaceAll(">", ">");
        body = body.replaceAll("&", "&");
        body = body.replaceAll("\"", "\"");
        body = body.replaceAll("'", "'");
        body = body.replaceAll("&nbsp;", " ");

        // 清理多余空白
        body = body.replaceAll("\n\\s+\n", "\n\n");
        body = body.replaceAll("\n{3,}", "\n\n");
        body = body.trim();

        return body;
    }

    /**
     * 处理 HTML 中的链接，将相对链接转换为绝对链接
     *
     * @param html    HTML 内容
     * @param baseUrl 基础 URL
     * @return 处理后的 HTML
     */
    protected String resolveLinks(String html, String baseUrl) {
        if (baseUrl == null || baseUrl.isEmpty()) {
            return html.replaceAll("(?i)<a[^>]*href=[\"']([^\"']*)[\"'][^>]*>(.*?)</a>", "[$2]($1)");
        }

        Pattern linkPattern = Pattern.compile("(?i)<a[^>]*href=[\"']([^\"']*)[\"'][^>]*>(.*?)</a>");
        Matcher matcher = linkPattern.matcher(html);

        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String href = matcher.group(1);
            String text = matcher.group(2);

            // 转换为绝对链接
            String absoluteHref = href;
            if (href.startsWith("/") && !href.startsWith("//")) {
                try {
                    URL base = new URL(baseUrl);
                    absoluteHref = base.getProtocol() + "://" + base.getHost() + href;
                } catch (MalformedURLException e) {
                    // 忽略转换错误，保持原链接
                }
            } else if (href.startsWith("#")) {
                // 锚点链接
                absoluteHref = baseUrl + href;
            } else if (!href.startsWith("http://") && !href.startsWith("https://") && !href.startsWith("//") && !href.startsWith("mailto:")) {
                // 相对路径
                try {
                    URL base = new URL(baseUrl);
                    String basePath = base.getPath();
                    if (basePath.endsWith("/")) {
                        absoluteHref = base.getProtocol() + "://" + base.getHost() + basePath + href;
                    } else {
                        int lastSlash = basePath.lastIndexOf('/');
                        if (lastSlash >= 0) {
                            basePath = basePath.substring(0, lastSlash + 1);
                        } else {
                            basePath = "/";
                        }
                        absoluteHref = base.getProtocol() + "://" + base.getHost() + basePath + href;
                    }
                } catch (MalformedURLException e) {
                    // 忽略转换错误
                }
            }

            String replacement = "[" + Matcher.quoteReplacement(text) + "](" + Matcher.quoteReplacement(absoluteHref) + ")";
            matcher.appendReplacement(result, replacement);
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * 兼容性方法：将HTML内容转换为Markdown格式（旧版本）
     *
     * @param html HTML格式的搜索结果
     * @return Markdown格式的搜索结果
     */
    protected String toMarkdown(String html) {
        return toMarkdown(html, null);
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
    public static Map<String, String> simulatorDeviceHeader() {
        Map<String, String> headers = new HashMap<>();
        headers.put(HeaderName.ACCEPT.getName(), "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");
        headers.put(HeaderName.ACCEPT_ENCODING.getName(), "gzip");
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
