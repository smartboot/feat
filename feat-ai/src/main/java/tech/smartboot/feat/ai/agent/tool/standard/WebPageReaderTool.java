package tech.smartboot.feat.ai.agent.tool.standard;

import com.alibaba.fastjson2.JSONObject;
import tech.smartboot.feat.ai.agent.search.Searcher;
import tech.smartboot.feat.ai.agent.tool.ToolExecutor;

/**
 * 网页内容读取工具，用于直接读取指定URL的网页内容并返回其文本内容
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class WebPageReaderTool implements ToolExecutor {

    private static final String NAME = "web_page_reader";
    private static final String DESCRIPTION = "读取指定URL的网页内容并返回其文本内容";

    @Override
    public String execute(JSONObject parameters) {
        String url = parameters.getString("url");

        if (url == null || url.isEmpty()) {
            return "错误：必须提供'url'参数";
        }

        try {
            // 使用现有的Searcher类来获取网页内容
            return Searcher.search(url);
        } catch (Throwable e) {
            return "读取网页内容时发生错误: " + e.getMessage();
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
                "    \"url\": {\n" +
                "      \"type\": \"string\",\n" +
                "      \"description\": \"要读取的网页URL\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"required\": [\"url\"]\n" +
                "}";
    }

    public static void main(String[] args) {
        WebPageReaderTool tool = new WebPageReaderTool();
        JSONObject params = new JSONObject();
        params.put("url", "https://www.oschina.net/news/385596/smart-mqtt-1-5-0-released");
        System.out.println(tool.execute(params));
    }
}