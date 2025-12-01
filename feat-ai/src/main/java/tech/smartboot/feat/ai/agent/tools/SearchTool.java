/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.agent.tools;

import com.alibaba.fastjson2.JSONObject;
import tech.smartboot.feat.ai.agent.AgentTool;
import tech.smartboot.feat.ai.agent.tools.reader.WebReader;
import tech.smartboot.feat.core.client.HttpGet;

import java.util.function.Consumer;

/**
 * 搜索工具，用于在互联网或特定数据源中搜索信息
 * <p>
 * 该工具允许AI Agent执行网络搜索操作，支持多种搜索引擎，
 * 包括百度和必应。搜索结果以适合AI处理的格式返回。
 * </p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class SearchTool implements AgentTool {

    private static final String NAME = "search";
    private static final String DESCRIPTION = "在互联网或特定数据源中搜索信息";

    /**
     * 搜索引擎枚举
     * <p>
     * 定义支持的搜索引擎类型，目前支持百度和必应。
     * </p>
     */
    public enum SearchEngine {
        /**
         * 百度搜索引擎
         */
        BAIDU("baidu"),

        /**
         * 必应搜索引擎
         */
        BING("bing");

        private final String value;

        SearchEngine(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        /**
         * 根据字符串值获取对应的搜索引擎枚举
         *
         * @param value 搜索引擎字符串值
         * @return 对应的搜索引擎枚举，如果找不到则返回百度
         */
        public static SearchEngine fromString(String value) {
            for (SearchEngine engine : SearchEngine.values()) {
                if (engine.value.equalsIgnoreCase(value)) {
                    return engine;
                }
            }
            return BAIDU; // 默认使用百度
        }
    }

    /**
     * 执行搜索操作
     * <p>
     * 根据提供的参数执行网络搜索，并返回格式化的搜索结果。
     * </p>
     *
     * @param parameters 包含搜索查询词、最大结果数和搜索引擎的参数
     * @return 搜索结果字符串
     */
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
                    return WebReader.read("https://cn.bing.com/search", httpGet -> httpGet.addQueryParam("q", query));
                case BAIDU:
                default:
                    return WebReader.read("https://www.baidu.com/s", new Consumer<HttpGet>() {
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

    /**
     * 获取工具名称
     *
     * @return 工具名称 "search"
     */
    @Override
    public String getName() {
        return NAME;
    }

    /**
     * 获取工具描述
     *
     * @return 工具功能描述
     */
    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    /**
     * 获取工具参数的JSON Schema定义
     * <p>
     * 定义了搜索工具的参数格式，包括查询词、最大结果数和搜索引擎选择。
     * </p>
     *
     * @return 参数定义的JSON Schema字符串
     */
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

    /**
     * 测试方法
     *
     * @param args 命令行参数
     * @throws Throwable 异常
     */
    public static void main(String[] args) throws Throwable {
        SearchTool tool = new SearchTool();

        // 测试百度搜索
        JSONObject params = new JSONObject();
        params.put("query", "smart-socket");
        params.put("engine", "baidu");
        System.out.println("=== 百度搜索结果 ===");
        System.out.println(tool.execute(params));

        // 测试必应搜索
        params.put("engine", "bing");
        System.out.println("\n=== 必应搜索结果 ===");
        System.out.println(tool.execute(params));
    }

}