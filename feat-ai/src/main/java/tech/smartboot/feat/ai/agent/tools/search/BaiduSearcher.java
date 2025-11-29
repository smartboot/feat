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

/**
 * 百度搜索引擎实现
 * <p>
 * 专门用于处理百度搜索结果的解析和格式化，将百度特有的HTML结构
 * 转换为标准的Markdown格式，便于AI处理和理解。
 * </p>
 *
 * @author 三刀
 * @version v1.0 11/26/25
 */
class BaiduSearcher extends Searcher {
    /**
     * 百度搜索的基础URL
     */
    public static final String BASE_URL = "https://www.baidu.com/s";

    /**
     * 将百度搜索返回的HTML内容转换为Markdown格式
     * <p>
     * 解析百度搜索结果页面，提取搜索结果条目，并将其转换为
     * Markdown格式的链接列表，便于AI阅读和处理。
     * </p>
     *
     * @param html 百度搜索结果的HTML内容
     * @return Markdown格式的搜索结果
     */
    @Override
    protected String toMarkdown(String html) {
        // 移除HTML中的script标签及其内容
        int startIndex = html.indexOf("id=\"content_left\"");
        html = html.substring(startIndex);
        html = html.substring(0, html.indexOf("<div style=\"clear:both;height:0;\"></div>"));
//            html = Pattern.compile("<style[^>]*>[\\s\\S]*?</style>", Pattern.CASE_INSENSITIVE).matcher(html).replaceAll("");
//            html = Pattern.compile("<!--[\\s\\S]*?-->", Pattern.CASE_INSENSITIVE).matcher(html).replaceAll("");
//            html = Pattern.compile("data-feedback=\"[\\s\\S]*?\"", Pattern.CASE_INSENSITIVE).matcher(html).replaceAll("");
        StringBuilder sb = new StringBuilder();
        int i = html.indexOf("class=\"result c-container");
        int j;
        while (i >= 0) {
            j = html.indexOf("class=\"result c-container", i + 1);
            if (j < 0) {
                sb.append(itemToMarkdown(html.substring(i)));
                break;
            } else {
                sb.append(itemToMarkdown(html.substring(i, j)));
                i = j;
            }
        }

//        html = Pattern.compile("<path [\\s\\S]*?/>", Pattern.CASE_INSENSITIVE).matcher(html).replaceAll("");
//        html = Pattern.compile("<meta [\\s\\S]*?/>", Pattern.CASE_INSENSITIVE).matcher(html).replaceAll("");
//        html = Pattern.comile("<div style=\"display:none\"[^>]*>[\\s\\S]*?</div>", Pattern.CASE_INSENSITIVE).matcher(html).replaceAll("");
        return sb.toString();
    }

    /**
     * 将单个百度搜索结果条目转换为Markdown格式
     * <p>
     * 解析单个搜索结果条目，提取标题、链接和描述信息，
     * 并格式化为Markdown链接格式。
     * </p>
     *
     * @param item 单个搜索结果条目的HTML内容
     * @return Markdown格式的搜索结果条目
     */
    private String itemToMarkdown(String item) {
        int i = item.indexOf("mu=\"");
        int j = item.indexOf("\"", i + 4);
        StringBuilder sb = new StringBuilder();
        String href = item.substring(i + 4, j);
        i = j;

        i = item.indexOf("<!--s-text-->", i);
        j = item.indexOf("<!--/s-text-->", i);
        String title = item.substring(i + 13, j);
        i = j;
        i = item.indexOf("<!--s-data:", i);
        i = item.indexOf("\"text\":\"", i);
        j = item.indexOf("\"}],", i);
        String description = item.substring(i + 8, j);

//            i = item.indexOf(">");
//            item = item.substring(i + 1);
//            i = item.indexOf("</p>");
        sb.append("- [").append(title.replaceAll("<strong>", "**").replaceAll("</strong>", "**")).append("](").append(href).append(") ").append("\r\n  ").append(description).append("\r\n");
//        sb.append("- [").append(href).append("](").append(href).append(") ");

        return sb.toString();
    }
}