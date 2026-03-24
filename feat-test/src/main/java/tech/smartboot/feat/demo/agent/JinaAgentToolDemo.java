/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.demo.agent;

import com.alibaba.fastjson2.JSONObject;
import tech.smartboot.feat.Feat;
import tech.smartboot.feat.ai.FeatAI;
import tech.smartboot.feat.ai.agent.AgentTool;
import tech.smartboot.feat.ai.agent.FeatAgent;
import tech.smartboot.feat.ai.chat.ChatModelVendor;
import tech.smartboot.feat.core.common.FeatUtils;

import java.util.concurrent.CompletableFuture;

/**
 * 使用 Jina AI 作为 Agent Tool 的实战演示
 * <p>
 * 【文档类型】实战教程
 * 【目的】演示如何将 Jina AI 的网页内容提取能力封装为 Feat Agent 的工具
 * 【前置条件】已配置 AI 模型（如 GiteeAI）
 * 【验证方式】运行 main 方法，观察 Agent 如何使用 Jina AI 工具分析网页内容
 * <p>
 * Jina AI 简介：
 * Jina AI 提供了强大的网页内容提取 API (https://r.jina.ai/http://example.com)，
 * 可以将任意网页内容转换为结构化的 Markdown 格式，非常适合用于：
 * 1. 网页内容摘要
 * 2. 信息提取
 * 3. 网页内容问答
 * 4. 数据抓取
 *
 * @author Feat Team
 * @version v1.0.0
 */
public class JinaAgentToolDemo {

    /**
     * 主入口
     */
    public static void main(String[] args) throws Exception {
        System.out.println("======================================================");
        System.out.println("  Feat AI + Jina AI 工具实战演示");
        System.out.println("======================================================\n");

        // 演示1: 基本的网页内容提取
        demonstrateBasicWebReader();

        System.out.println("\n=====================================================\n");

        // 演示2: Agent 使用 Jina AI 工具分析网页
        demonstrateAgentWithJinaTool();

        System.out.println("\n======================================================");
        System.out.println("  演示完成");
        System.out.println("======================================================");
    }

    /**
     * 演示1: 基本的网页内容提取
     * <p>
     * 直接使用 JinaReaderTool 提取网页内容
     */
    private static void demonstrateBasicWebReader() throws Exception {
        System.out.println("【演示1】基本网页内容提取");
        System.out.println("-------------------------------------------------------\n");

        // 创建 Jina AI 工具实例
        JinaReaderTool jinaTool = new JinaReaderTool();

        // 准备一个测试 URL（以 Jina AI 官网为例）
        String url = "https://jina.ai";

        System.out.println("正在提取网页内容: " + url);
        System.out.println("-------------------------------------------------------");

        // 构建参数
        JSONObject params = new JSONObject();
        params.put("url", url);

        // 执行工具
        String result = jinaTool.execute(params).get();

        System.out.println("\n[完整内容长度: " + result.length() + " 字符]");
        System.out.println(result);
    }

    /**
     * 演示2: Agent 使用 Jina AI 工具分析网页
     * <p>
     * 创建一个 Agent，并为其注册 Jina AI 工具，让 Agent 能够读取并分析网页内容
     */
    private static void demonstrateAgentWithJinaTool() throws Exception {
        System.out.println("【演示2】Agent 使用 Jina AI 工具分析网页");
        System.out.println("-------------------------------------------------------\n");

        // 创建 Agent 并注册 Jina AI 工具
        FeatAgent agent = FeatAI.agent(opts -> {
            // 配置对话模型
            opts.chatOptions().model(ChatModelVendor.GiteeAI.Qwen2_5_72B_Instruct);

            // 注册 Jina AI 网页读取工具
            opts.tool(new JinaReaderTool());

            // 可选：设置最大迭代次数
            opts.maxIterations(10);
        });

        // 测试任务：分析一个网页并提取关键信息
        String[] tasks = {
                "请帮我查看 https://jina.ai 这个网站，告诉我这是做什么的公司",
                "访问 https://smartboot.tech 并总结这个网站的主要内容"
        };

        for (String task : tasks) {
            System.out.println("任务: " + task);
            System.out.println("-------------------------------------------------------");

            try {
                // 执行任务
                String result = agent.execute(task).get();

                System.out.println("Agent 回复:");
                System.out.println(result);
                System.out.println("\nAgent 状态: " + agent.getState());

            } catch (Exception e) {
                System.out.println("执行失败: " + e.getMessage());
                e.printStackTrace();
            }

            System.out.println("\n");
        }
    }

    /**
     * Jina AI 网页读取工具
     * <p>
     * 实现 AgentTool 接口，封装 Jina AI 的网页内容提取能力
     * Jina AI API: https://r.jina.ai/http://目标URL
     */
    public static class JinaReaderTool implements AgentTool {

        private static final String NAME = "jina_web_reader";
        private static final String DESCRIPTION = "使用 Jina AI 读取任意网页的内容并返回结构化文本。适用于获取网页文章、博客、文档等内容。";

        /**
         * 执行网页内容读取
         *
         * @param parameters 包含目标 URL 的参数
         * @return 网页的结构化文本内容
         */
        @Override
        public CompletableFuture<String> execute(JSONObject parameters) {
            String url = parameters.getString("url");

            if (FeatUtils.isBlank(url)) {
                return CompletableFuture.completedFuture("错误：必须提供 'url' 参数");
            }

            // 构建 Jina AI 请求 URL
            String jinaUrl = buildJinaUrl(url);

            // 使用 Feat 的 HTTP 客户端发送请求
            return Feat.httpClient(jinaUrl, httpOptions -> httpOptions.debug(false))
                    .get().submit().thenApply(response -> {
                        String content = response.body();

                        if (content == null || content.isEmpty()) {
                            return "错误：无法获取网页内容，返回为空";
                        }

                        // 检查 Jina AI 的错误响应
                        if (content.startsWith("{")) {
                            try {
                                JSONObject errorJson = JSONObject.parseObject(content);
                                if (errorJson.containsKey("error")) {
                                    return "Jina AI 错误: " + errorJson.getString("error");
                                }
                            } catch (Exception e) {
                                // 不是 JSON，继续处理
                            }
                        }

                        // 构建结果
                        StringBuilder result = new StringBuilder();
                        result.append("网页 URL: ").append(url).append("\n");
                        result.append("----------------------------------------\n");
                        result.append(content);
                        result.append("\n----------------------------------------");
                        return result.toString();
                    }).exceptionally(throwable -> "执行失败: " + throwable.getMessage());
        }

        /**
         * 构建 Jina AI 的请求 URL
         */
        private String buildJinaUrl(String targetUrl) {
            // 如果 URL 已经包含协议，则去掉协议部分
            if (targetUrl.startsWith("http://")) {
                return "https://r.jina.ai/http://" + targetUrl.substring(7);
            } else if (targetUrl.startsWith("https://")) {
                return "https://r.jina.ai/https://" + targetUrl.substring(8);
            } else {
                // 默认添加 https://
                return "https://r.jina.ai/https://" + targetUrl;
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
                    "      \"description\": \"要读取的网页 URL，支持 http:// 或 https:// 开头的完整 URL\"\n" +
                    "    }\n" +
                    "  },\n" +
                    "  \"required\": [\"url\"]\n" +
                    "}";
        }
    }
}
