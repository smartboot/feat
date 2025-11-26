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

/**
 * @author 三刀
 * @version v1.0 11/26/25
 */
class BaiduSearcher extends Searcher {
    public static final String BASE_URL = "https://www.baidu.com/s";


    @Override
    protected String toMarkdown(String html) {
        // 移除HTML中的script标签及其内容
        int startIndex = html.indexOf("id=\"content_left\"");
        html = html.substring(startIndex);
        html = html.substring(0, html.indexOf("<div style=\"clear:both;height:0;\"></div>"));
//            html = Pattern.compile("<style[^>]*>[\\s\\S]*?</style>", Pattern.CASE_INSENSITIVE).matcher(html).replaceAll("");
//            html = Pattern.compile("<!--[\\s\\S]*?-->", Pattern.CASE_INSENSITIVE).matcher(html).replaceAll("");
//            html = Pattern.compile("data-feedback=\"[\\s\\S]*?\"", Pattern.CASE_INSENSITIVE).matcher(html).replaceAll("");
        System.out.println(html);
        System.out.println();
        System.out.println();
        System.out.println();
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
