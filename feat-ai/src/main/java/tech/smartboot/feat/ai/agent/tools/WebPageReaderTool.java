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

/**
 * 网页内容读取工具，用于直接读取指定URL的网页内容并返回其文本内容
 * <p>
 * 该工具允许AI Agent读取指定网页的完整内容，并将其转换为纯文本格式返回，
 * 便于后续处理和分析。工具会自动处理HTML标签，提取纯文本内容。
 * </p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class WebPageReaderTool implements AgentTool {

    private static final String NAME = "web_page_reader";
    private static final String DESCRIPTION = "读取指定URL的网页内容并返回其文本内容";

    /**
     * 执行网页内容读取操作
     * <p>
     * 读取指定URL的网页内容，自动处理HTML并提取纯文本内容。
     * </p>
     *
     * @param parameters 包含目标URL的参数
     * @return 网页的纯文本内容
     */
    @Override
    public String execute(JSONObject parameters) {
        String url = parameters.getString("url");

        if (url == null || url.isEmpty()) {
            return "错误：必须提供'url'参数";
        }

        try {
            // 使用现有的Searcher类来获取网页内容
            return WebReader.read(url);
        } catch (Throwable e) {
            return "读取网页内容时发生错误: " + e.getMessage();
        }
    }

    /**
     * 获取工具名称
     *
     * @return 工具名称 "web_page_reader"
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
     * 定义了网页读取工具的参数格式，只需要目标URL。
     * </p>
     *
     * @return 参数定义的JSON Schema字符串
     */
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

    /**
     * 测试方法
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        WebPageReaderTool tool = new WebPageReaderTool();
        JSONObject params = new JSONObject();
        params.put("url", "https://www.oschina.net/");
        System.out.println(tool.execute(params));
    }
}