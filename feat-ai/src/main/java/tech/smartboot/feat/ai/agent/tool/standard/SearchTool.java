package tech.smartboot.feat.ai.agent.tool.standard;

import com.alibaba.fastjson2.JSONObject;
import tech.smartboot.feat.ai.agent.search.Searcher;
import tech.smartboot.feat.ai.agent.tool.ToolExecutor;
import tech.smartboot.feat.core.client.HttpGet;

import java.util.function.Consumer;

/**
 * 搜索工具，用于在互联网或特定数据源中搜索信息
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class SearchTool implements ToolExecutor {

    private static final String NAME = "search";
    private static final String DESCRIPTION = "在互联网或特定数据源中搜索信息";

    public enum SearchEngine {
        BAIDU("baidu"),
        BING("bing");

        private final String value;

        SearchEngine(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static SearchEngine fromString(String value) {
            for (SearchEngine engine : SearchEngine.values()) {
                if (engine.value.equalsIgnoreCase(value)) {
                    return engine;
                }
            }
            return BAIDU; // 默认使用百度
        }
    }

//    private final BaiduSearch baiduSearch = new BaiduSearch();
//    private final BingSearch bingSearch = new BingSearch();

    @Override
    public String execute(JSONObject parameters) {
        String query = parameters.getString("query");
        Integer maxResults = parameters.getInteger("max_results");
        String engine = parameters.getString("engine");

        if (query == null || query.isEmpty()) {
            return "错误：必须提供'query'参数";
        }

        if (maxResults == null) {
            maxResults = 5; // 默认返回5个结果
        }

        SearchEngine searchEngine = SearchEngine.fromString(engine);

        try {
            switch (searchEngine) {
                case BING:
                    return Searcher.search("https://cn.bing.com/search", httpGet -> httpGet.addQueryParam("q", query));
                case BAIDU:
                default:
                    return Searcher.search("https://www.baidu.com/s", new Consumer<HttpGet>() {
                        @Override
                        public void accept(HttpGet httpGet) {
                            httpGet.addQueryParam("wd", query)
                                    .addQueryParam("ie", "utf-8")
                                    .addQueryParam("rsv_spt", "1");
                        }
                    });
            }
        } catch (Throwable e) {
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
                "    },\n" +
                "    \"engine\": {\n" +
                "      \"type\": \"string\",\n" +
                "      \"description\": \"搜索引擎(baidu|bing)\",\n" +
                "      \"enum\": [\"baidu\", \"bing\"]\n" +
                "    }\n" +
                "  },\n" +
                "  \"required\": [\"query\"]\n" +
                "}";
    }

    public static void main(String[] args) throws Throwable {
        SearchTool tool = new SearchTool();

        // 测试百度搜索
        JSONObject params = new JSONObject();
        params.put("query", "番茄炒蛋");
        params.put("engine", "baidu");
        System.out.println("=== 百度搜索结果 ===");
        System.out.println(tool.execute(params));

        // 测试必应搜索
        params.put("engine", "bing");
        System.out.println("\n=== 必应搜索结果 ===");
//        System.out.println(tool.execute(params));
    }

}