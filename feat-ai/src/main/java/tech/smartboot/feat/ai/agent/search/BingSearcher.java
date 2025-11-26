/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.agent.search;

import java.util.regex.Pattern;

/**
 * @author 三刀
 * @version v1.0 11/26/25
 */
class BingSearcher extends Searcher {
    public static final String BASE_URL = "https://cn.bing.com/search";


    @Override
    protected String toMarkdown(String html) {
        // 移除HTML中的script标签及其内容
        int startIndex = html.indexOf("<main");
        html = html.substring(startIndex);
        html = html.substring(0, html.lastIndexOf("</main>") + 7);
        html = html.substring(html.indexOf("<ol id=\"b_results\""));
        html = html.substring(0, html.lastIndexOf("</ol>") + 5);
//                    html = html.substring(html.indexOf("<li"));
//                    html = html.substring(0, html.lastIndexOf("</li>"));
        html = Pattern.compile("<style[^>]*>[\\s\\S]*?</style>", Pattern.CASE_INSENSITIVE).matcher(html).replaceAll("");
        System.out.println(html);
        System.out.println();
        System.out.println();
        System.out.println();
        StringBuilder sb = new StringBuilder();
        int i = html.indexOf("class=\"b_algoheader\"");
        int j;
        while (i >= 0) {
            j = html.indexOf("class=\"b_algoheader\"", i + 1);
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

    private String itemToMarkdown(String item) {
        int i = item.indexOf("b_algoheader");
        if (i == -1) {
            return "";
        }
        item = item.substring(item.indexOf("href") + 6);
        StringBuilder sb = new StringBuilder();
        String href = item.substring(0, item.indexOf("\""));
        item = item.substring(item.indexOf("<h2") + 1);
        item = item.substring(item.indexOf(">") + 1);
        i = item.indexOf("</h2>");
        String title = item.substring(0, i);
        i = item.indexOf("<p");
        item = item.substring(i + 2);
        i = item.indexOf(">");
        item = item.substring(i + 1);
        i = item.indexOf("</p>");
        String description = item.substring(0, i);
        sb.append("- [").append(title.replaceAll("<strong>", "**").replaceAll("</strong>", "**")).append("](").append(href).append(") ").append("\r\n  ").append(description).append("\r\n");
//        sb.append("- [").append(href).append("](").append(href).append(") ");

        return sb.toString();
    }
}
