/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.agent.tools.search;

import tech.smartboot.feat.core.client.HttpGet;
import tech.smartboot.feat.core.common.HeaderName;
import tech.smartboot.feat.core.common.HeaderValue;

/**
 * 默认搜索引擎实现
 * <p>
 * 用于处理通用网页内容的解析和格式化，将HTML内容转换为Markdown格式，
 * 适用于无法识别的搜索引擎或普通网页内容的处理。
 * </p>
 *
 * @author 三刀
 * @version v1.0 11/26/25
 */
public class DefaultSearcher extends Searcher {

    /**
     * 初始化HTTP请求
     * <p>
     * 设置HTTP连接为keep-alive，提高请求效率。
     * </p>
     *
     * @param httpGet HTTP GET请求对象
     */
    @Override
    protected void initRequest(HttpGet httpGet) {
        httpGet.header().set(HeaderName.CONNECTION, HeaderValue.Connection.keepalive);
    }

    /**
     * 将HTML内容转换为Markdown格式
     * <p>
     * 通用的HTML到Markdown转换器，处理各种HTML标签并转换为对应的Markdown格式：
     * - 标题标签(h1-h6)转换为Markdown标题
     * - 段落标签(p)转换为文本段落
     * - 链接标签(a)转换为Markdown链接
     * - 列表标签(ul, ol, li)转换为Markdown列表
     * - 代码标签(code, pre)转换为Markdown代码块
     * - 等等
     * </p>
     *
     * @param html HTML格式的内容
     * @return Markdown格式的内容
     */
    @Override
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
}