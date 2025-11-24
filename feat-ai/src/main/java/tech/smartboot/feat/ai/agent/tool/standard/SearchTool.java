/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.agent.tool.standard;

import com.alibaba.fastjson2.JSONObject;
import tech.smartboot.feat.ai.agent.tool.ToolExecutor;

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
        
        // 这里应该实际执行搜索操作
        // 目前我们只是模拟这个过程
        return String.format("搜索查询 '%s' 的结果（最多 %d 个结果）:\n" +
                "1. 相关结果1\n" +
                "2. 相关结果2\n" +
                "3. 相关结果3\n" +
                "注意：这是一个模拟的搜索工具，实际实现需要连接到搜索引擎API", query, maxResults);
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
}