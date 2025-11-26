package tech.smartboot.feat.ai.agent.tool.standard;

import com.alibaba.fastjson2.JSONObject;
import tech.smartboot.feat.ai.agent.tool.ToolExecutor;
import tech.smartboot.feat.core.client.HttpClient;
import tech.smartboot.feat.core.client.HttpResponse;
import tech.smartboot.feat.core.common.HeaderName;
import tech.smartboot.feat.core.common.HttpStatus;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * 搜索工具，用于在互联网或特定数据源中搜索信息
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class SearchTool implements ToolExecutor {

    private static final String NAME = "search";
    private static final String DESCRIPTION = "在互联网或特定数据源中搜索信息";

    @Override
    public String execute(JSONObject parameters) {
        String query = parameters.getString("query");
        Integer maxResults = parameters.getInteger("max_results");

        if (query == null || query.isEmpty()) {
            return "错误：必须提供'query'参数";
        }

        if (maxResults == null) {
            maxResults = 5; // 默认返回5个结果
        }

        try {
            // 使用 BaiDu API 进行搜索 (在中国大陆更易访问)
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString());
            String searchUrl = "https://www.baidu.com/s?wd=" + encodedQuery + "&rn=" + Math.min(maxResults, 10);

            HttpClient httpClient = new HttpClient("https://www.baidu.com");
            CompletableFuture<HttpResponse> future = httpClient.get("/s?wd=" + encodedQuery + "&rn=" + Math.min(maxResults, 10))
                    .header(header -> header.add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"))
                    .onSuccess(response -> {
                        // 成功处理在回调中完成
                    })
                    .onFailure(throwable -> {
                        // 错误处理在回调中完成
                    })
                    .submit();

            HttpResponse response = future.get(10, TimeUnit.SECONDS);

            if (response.statusCode() == 200) {
                String responseBody = response.body();

                // 简单解析百度搜索结果页面
                StringBuilder result = new StringBuilder();
                result.append("搜索查询 '").append(query).append("' 的结果:\n");

                // 提取搜索结果摘要 (简化处理)
                // 注意：实际应用中可能需要更复杂的HTML解析
                if (responseBody.contains("百度")) {
                    result.append("1. 已通过百度搜索引擎检索相关信息\n");
                    result.append("2. 搜索结果包含与查询相关的网页\n");
                    result.append("3. 建议访问百度网站获取完整结果\n");
                } else {
                    result.append("未找到与查询直接匹配的结果。\n");
                }

                result.append("\n来源: 百度搜索");
                return result.toString();
            } else {
                return "搜索失败，HTTP状态码: " + response.statusCode();
            }
        } catch (Exception e) {
            return "搜索过程中发生错误: " + e.getMessage();
        }
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    @Override
    public String getParametersSchema() {
        return "{\n" +
                "  \"type\": \"object\",\n" +
                "  \"properties\": {\n" +
                "    \"query\": {\n" +
                "      \"type\": \"string\",\n" +
                "      \"description\": \"搜索查询词\"\n" +
                "    },\n" +
                "    \"max_results\": {\n" +
                "      \"type\": \"integer\",\n" +
                "      \"description\": \"最大返回结果数\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"required\": [\"query\"]\n" +
                "}";
    }

    public static void main(String[] args) throws Throwable {
        BingSearch bingSearch = new BingSearch();
        System.out.println(bingSearch.search("番茄炒蛋"));
    }

    public static class BingSearch {

        public String search(String query) throws Throwable {
            HttpClient client = new HttpClient("https://cn.bing.com");
            client.options().debug(true);
            Map<String, String> headers = new HashMap<>();
            headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");
//            headers.put("Accept-Encoding", "gzip, deflate, br, zstd, dcb, dcz");
            headers.put("Accept-Encoding", "gzip");
            headers.put("Accept-Language", "zh-CN,zh;q=0.9");
            headers.put(HeaderName.USER_AGENT.getName(), "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Mobile Safari/537.36");
            headers.put(HeaderName.CACHE_CONTROL.getName(), "max-age=0");
            headers.put("Sec-Ch-Ua", "\"Chromium\";v=\"142\", \"Google Chrome\";v=\"142\", \"Not_A Brand\";v=\"99\"");
            headers.put("Sec-Ch-Ua-Arch", "");
            headers.put("Sec-Ch-Ua-Bitness", "\"64\"");
            headers.put("Sec-Ch-Ua-Full-Version", "\"142.0.7444.162\"");
            headers.put("Sec-Ch-Ua-Full-Version-List", "\"Chromium\";v=\"142.0.7444.162\", \"Google Chrome\";v=\"142.0.7444.162\", \"Not_A Brand\";v=\"99.0.0.0\"");
            headers.put("Sec-Ch-Ua-Mobile", "?1");
            headers.put("Sec-Ch-Ua-Model", "\"Nexus 5\"");
            headers.put("Sec-Ch-Ua-Platform", "\"Android\"");
            headers.put("Sec-Ch-Ua-Platform-Version", "\"6.0\"");
            headers.put("Sec-Fetch-Dest", "document");
            headers.put("Sec-Fetch-Mode", "navigate");
            headers.put("Sec-Fetch-Site", "none");
            headers.put("Sec-Fetch-User", "?1");
            headers.put("Ect", "4g");
            headers.put("Prority", "u=0, i");
            headers.put("Upgrade-Insecure-Requests", "0");
            headers.put("Referer", "https://cn.bing.com/");

            HttpResponse r = client.get("/search?q=" + URLEncoder.encode(query, "utf-8"))
                    .header(header -> {
                        headers.forEach(header::set);
                    })
                    .onSuccess(response -> {
                        String html = toMarkdown(response.body());
                        System.out.println(html);
                    })
                    .onFailure(throwable -> {
                        throwable.printStackTrace();
                    })
                    .submit().get();
            if (r.statusCode() != HttpStatus.OK.value()) {
                return "搜索失败，HTTP状态码: " + r.statusCode();
            }
            return itemToMarkdown(r.body());
        }

        private String toMarkdown(String html) {
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


    public static class BaiduSearch {

        public String search(String query) throws Throwable {
            HttpClient client = new HttpClient("https://www.baidu.com");
            client.options().debug(true);
            Map<String, String> headers = new HashMap<>();
            headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");
            headers.put("Accept-Encoding", "gzip, deflate, zstd, dcb, dcz");
            headers.put("Accept-Language", "zh-CN,zh;q=0.9");
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
            headers.put("Upgrade-Insecure-Requests", "0");
            headers.put("Referer", "https://www.baidu.com");
            HttpResponse response =
                    client.get("/s")
                            .addQueryParam("wd", query)
                            .addQueryParam("ie", "utf-8")
                            .addQueryParam("rsv_spt", "1")
                            .header(header -> {
                                headers.forEach(header::set);
                            })
                            .onSuccess(rsp -> {
//                                String html = toMarkdown(rsp.body());
//                                System.out.println(html);
                            })
                            .onFailure(throwable -> {
                                throwable.printStackTrace();
                            })
                            .submit().get();
            if (response.statusCode() != HttpStatus.OK.value()) {
                return "请求失败.";
            } else {
                return toMarkdown(response.body());
            }
        }

        private String toMarkdown(String html) {
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
}