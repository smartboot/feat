/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.demo.featclaw;

import tech.smartboot.feat.ai.FeatAI;
import tech.smartboot.feat.ai.agent.AgentOptions;
import tech.smartboot.feat.ai.agent.FeatAgent;
import tech.smartboot.feat.ai.agent.tools.FileOperationTool;
import tech.smartboot.feat.ai.agent.tools.SearchTool;
import tech.smartboot.feat.ai.agent.tools.WebPageReaderTool;
import tech.smartboot.feat.ai.chat.ChatModelVendor;
import tech.smartboot.feat.ai.chat.entity.Message;
import tech.smartboot.feat.ai.chat.prompt.Prompt;
import tech.smartboot.feat.core.common.FeatUtils;
import tech.smartboot.feat.demo.featclaw.tools.CodeGeneratorTool;
import tech.smartboot.feat.demo.featclaw.tools.ProjectAnalyzerTool;
import tech.smartboot.feat.demo.featclaw.tools.ShellExecuteTool;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * FeatClaw 启动类 - 交互式 AI 开发助手
 * <p>
 * FeatClaw 是一个基于 FeatAI.agent() 方法创建的软件开发助手，类似于 OpenClaw。
 * 它提供了一个交互式命令行界面，帮助开发者：
 * 1. 分析项目结构和代码
 * 2. 生成高质量的 Java 代码
 * 3. 执行 Maven 构建和测试
 * 4. 搜索技术文档和解决方案
 * </p>
 * <p>
 * 使用方法：
 * 1. 直接运行 main 方法启动交互式会话
 * 2. 输入问题或指令与 FeatClaw 对话
 * 3. 输入 'exit' 或 'quit' 退出程序
 * </p>
 *
 * @author Feat Team
 * @version v1.0.0
 */
public class Bootstrap {

    /**
     * FeatClaw 专用提示词模板
     */
    private static final Prompt FEATCLAW_PROMPT = new Prompt(
        FeatUtils.getResourceAsString("feat-prompts/featclaw_agent.tpl")
    );

    /**
     * 欢迎信息
     */
    private static final String WELCOME_MESSAGE = 
        "============================================================\n" +
        "                                                            \n" +
        "              FeatClaw - AI 开发助手                        \n" +
        "                                                            \n" +
        "   基于 FeatAgent 的智能编程助手，助你高效开发               \n" +
        "                                                            \n" +
        "============================================================\n\n" +
        "FeatClaw 可以帮助你:\n" +
        "  [分析] 分析项目结构和代码\n" +
        "  [生成] 生成 Java 类和配置文件\n" +
        "  [执行] 执行 Maven 命令和 Shell 脚本\n" +
        "  [搜索] 搜索技术文档和解决方案\n\n" +
        "使用提示:\n" +
        "  - 直接输入你的问题或需求\n" +
        "  - 输入 'exit' 或 'quit' 退出\n" +
        "  - 输入 'clear' 清空对话历史\n" +
        "  - 输入 'help' 显示帮助信息\n\n";

    /**
     * 帮助信息
     */
    private static final String HELP_MESSAGE =
        "\n============================================================\n" +
        "                  帮助信息                                   \n" +
        "============================================================\n\n" +
        "FeatClaw 支持以下类型的任务:\n\n" +
        "1. 项目分析:\n" +
        "   - 分析项目结构和依赖\n" +
        "   - 查看 pom.xml 配置\n" +
        "   - 统计代码行数\n\n" +
        "2. 代码生成:\n" +
        "   - 生成 Controller 类\n" +
        "   - 生成 Service 类\n" +
        "   - 生成 Entity 类\n" +
        "   - 生成配置文件\n\n" +
        "3. 命令执行:\n" +
        "   - Maven 构建 (mvn clean install)\n" +
        "   - Git 操作 (git status, git log)\n" +
        "   - 其他安全命令\n\n" +
        "4. 网络搜索:\n" +
        "   - 搜索技术问题\n" +
        "   - 查阅文档和教程\n\n" +
        "快捷命令:\n" +
        "  help   - 显示此帮助信息\n" +
        "  clear  - 清空对话历史\n" +
        "  exit   - 退出程序\n" +
        "  quit   - 退出程序\n\n";

    public static void main(String[] args) {
        System.out.println(WELCOME_MESSAGE);

        // 使用 FeatAI.agent() 方法创建 FeatClaw Agent
        FeatAgent agent = FeatAI.agent(opts -> {
            // 配置 AI 模型
            opts.chatOptions()
                .model(ChatModelVendor.GiteeAI.DeepSeek_V32)
//                .temperature(0.7f)
            ;
            
            // 使用 FeatClaw 专用提示词模板
            opts.prompt(FEATCLAW_PROMPT);
            
            // 启用核心工具
            opts.tool(new FileOperationTool());
            opts.tool(new CodeGeneratorTool());
            opts.tool(new ProjectAnalyzerTool());
            opts.tool(new ShellExecuteTool());
            opts.tool(new SearchTool());
            opts.tool(new WebPageReaderTool());
            
            // 启用记忆功能
            opts.enableMemory();
        });

        // 对话历史（用于保持上下文）
        List<Message> conversationHistory = new ArrayList<Message>();
        
        // 添加系统提示
        conversationHistory.add(Message.ofSystem(
            "你是 FeatClaw，一个智能软件开发助手。你可以帮助用户分析项目、生成代码、" +
            "执行命令和搜索信息。请用中文回复用户，并提供详细、准确的帮助。"
        ));

        // 读取用户输入
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        
        try {
            while (true) {
                System.out.print("\nFeatClaw > ");
                String userInput = reader.readLine();
                
                if (userInput == null) {
                    break;
                }
                
                userInput = userInput.trim();
                
                // 处理特殊命令
                if (userInput.isEmpty()) {
                    continue;
                }
                
                String lowerInput = userInput.toLowerCase();
                if (lowerInput.equals("exit") || lowerInput.equals("quit")) {
                    System.out.println("\n感谢使用 FeatClaw，再见！\n");
                    break;
                }
                
                if (lowerInput.equals("clear")) {
                    conversationHistory.clear();
                    conversationHistory.add(Message.ofSystem(
                        "你是 FeatClaw，一个智能软件开发助手。"
                    ));
                    System.out.println("\n对话历史已清空。\n");
                    continue;
                }
                
                if (lowerInput.equals("help")) {
                    System.out.println(HELP_MESSAGE);
                    continue;
                }

                // 添加用户消息到历史
                conversationHistory.add(Message.ofUser(userInput));
                
                // 显示处理中提示
                System.out.println("\n思考中...\n");
                
                try {
                    // 执行 Agent
                    CompletableFuture<String> future = agent.execute(conversationHistory);
                    String response = future.get();
                    
                    // 添加助手回复到历史
                    conversationHistory.add(Message.ofAssistant(response));
                    
                    // 显示响应
                    System.out.println("============================================================\n");
                    System.out.println(response);
                    System.out.println("\n============================================================");
                    
                } catch (Exception e) {
                    System.err.println("\n错误: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            System.err.println("发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
