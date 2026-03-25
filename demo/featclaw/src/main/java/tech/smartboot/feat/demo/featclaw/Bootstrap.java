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
import tech.smartboot.feat.ai.agent.FeatAgent;
import tech.smartboot.feat.ai.chat.entity.Message;
import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;
import tech.smartboot.feat.demo.featclaw.config.ConfigManager;
import tech.smartboot.feat.demo.featclaw.coordination.AgentCoordinator;
import tech.smartboot.feat.demo.featclaw.registry.AgentRegistry;
import tech.smartboot.feat.demo.featclaw.registry.SkillRegistry;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * FeatClaw 启动类 - 多Agent协同的交互式 AI 开发助手
 * <p>
 * FeatClaw 基于外部配置文件（~/.featclaw/）管理多Agent，支持：
 * 1. Agent配置：~/.featclaw/agents/*.yaml
 * 2. Skill配置：~/.featclaw/skills/*\/skill.yaml
 * 3. 首次运行时自动从resources复制默认配置
 * </p>
 *
 * @author Feat Team
 * @version v2.0.0
 */
public class Bootstrap {
    
    private static final Logger logger = LoggerFactory.getLogger(Bootstrap.class);

    /**
     * 欢迎信息
     */
    private static final String WELCOME_MESSAGE = 
        "============================================================\n" +
        "                                                            \n" +
        "              FeatClaw - 多Agent协同开发助手                 \n" +
        "                                                            \n" +
        "   配置文件位置: ~/.featclaw/                                \n" +
        "                                                            \n" +
        "============================================================\n\n" +
        "FeatClaw 功能:\n" +
        "  🔍 [分析] 项目结构、依赖关系、代码统计\n" +
        "  💻 [生成] Java类、Controller、Service、配置文件\n" +
        "  🔧 [执行] Maven、Git 等安全命令\n" +
        "  🌐 [搜索] 技术文档和解决方案\n" +
        "  🤖 [协同] 多Agent智能协作完成复杂任务\n\n" +
        "快捷命令:\n" +
        "  agents    - 查看Agent详情\n" +
        "  skills    - 查看技能列表\n" +
        "  config    - 查看配置信息\n" +
        "  help      - 显示帮助信息\n" +
        "  clear     - 清空对话历史\n" +
        "  exit      - 退出程序\n\n" +
        "直接调用Agent: @agent-name: 任务描述\n\n";

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
        "4. 技术调研:\n" +
        "   - 搜索技术问题\n" +
        "   - 查阅文档和教程\n\n" +
        "5. 多Agent协作:\n" +
        "   - 复杂任务自动分解\n" +
        "   - 多Agent并行执行\n" +
        "   - 结果自动整合\n\n" +
        "快捷命令:\n" +
        "  help    - 显示此帮助信息\n" +
        "  agents  - 查看Agent详情\n" +
        "  skills  - 查看技能列表\n" +
        "  config  - 查看配置信息\n" +
        "  clear   - 清空对话历史\n" +
        "  exit    - 退出程序\n" +
        "  quit    - 退出程序\n\n" +
        "直接调用Agent:\n" +
        "  @project-analyzer: 分析当前项目结构\n" +
        "  @code-generator: 生成一个UserController\n" +
        "  @command-executor: 执行mvn clean install\n" +
        "  @research-assistant: 搜索Feat框架文档\n\n";

    /**
     * Agent协调器
     */
    private static AgentCoordinator coordinator;
    
    /**
     * 调度器Agent
     */
    private static FeatAgent orchestratorAgent;
    
    /**
     * 对话历史（用于保持上下文）
     */
    private static List<Message> conversationHistory = new ArrayList<>();
    
    /**
     * 配置管理器
     */
    private static ConfigManager configManager;
    
    /**
     * Agent注册表
     */
    private static AgentRegistry agentRegistry;
    
    /**
     * Skill注册表
     */
    private static SkillRegistry skillRegistry;

    public static void main(String[] args) {
        System.out.println(WELCOME_MESSAGE);
        
        // 初始化系统
        initialize();

        // 读取用户输入
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        try {
            while (true) {
                System.out.print("\n🦀 FeatClaw > ");
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
                
                // 退出命令
                if (lowerInput.equals("exit") || lowerInput.equals("quit")) {
                    System.out.println("\n感谢使用 FeatClaw，再见！\n");
                    break;
                }

                // 清空历史
                if (lowerInput.equals("clear")) {
                    conversationHistory.clear();
                    System.out.println("\n对话历史已清空。\n");
                    continue;
                }

                // 帮助命令
                if (lowerInput.equals("help")) {
                    System.out.println(HELP_MESSAGE);
                    continue;
                }
                
                // 查看配置信息
                if (lowerInput.equals("config")) {
                    configManager.printConfigInfo();
                    continue;
                }
                
                // 查看Agent
                if (lowerInput.equals("agents")) {
                    agentRegistry.printAgentInfo();
                    continue;
                }
                
                // 查看Skills
                if (lowerInput.equals("skills")) {
                    skillRegistry.printSkillInfo();
                    continue;
                }
                
                // 直接调用特定Agent的命令 @agentName: task
                if (userInput.startsWith("@")) {
                    handleDirectAgentCall(userInput);
                    continue;
                }

                // 使用调度器处理用户请求
                processWithOrchestrator(userInput);
            }
        } catch (Exception e) {
            System.err.println("发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 初始化系统
     */
    private static void initialize() {
        logger.info("正在初始化 FeatClaw 系统...");
        
        // 初始化配置管理器（会创建外部配置目录并复制默认配置）
        configManager = ConfigManager.getInstance();
        
        // 初始化注册表（从外部配置目录加载）
        agentRegistry = AgentRegistry.getInstance();
        skillRegistry = SkillRegistry.getInstance();
        
        // 打印加载信息
        System.out.println("📁 配置目录: " + configManager.getConfigRootDir().getAbsolutePath());
        System.out.println("🤖 已加载 " + agentRegistry.getAgentCount() + " 个Agent配置");
        System.out.println("🎯 已加载 " + skillRegistry.getSkillCount() + " 个技能配置\n");
        
        // 创建并初始化Agent协调器
        coordinator = new AgentCoordinator();
        coordinator.setGlobalConfig(opts -> {
            // 配置AI模型
            opts.chatOptions()

                    .model("kimi-k2.5")
                    .temperature(0.7f);
        });
        
        // 初始化所有Agent实例
        coordinator.initializeAgents();
        
        // 获取调度器Agent
        orchestratorAgent = coordinator.getOrchestratorAgent();
        if (orchestratorAgent == null) {
            // 如果没有配置调度器，使用默认方式创建
            orchestratorAgent = createDefaultOrchestrator();
        }
        
        logger.info("FeatClaw 系统初始化完成");
    }
    
    /**
     * 创建默认调度器Agent
     */
    private static FeatAgent createDefaultOrchestrator() {
        return FeatAI.agent(opts -> {
            opts.chatOptions()
                    .baseUrl("https://coding.dashscope.aliyuncs.com/v1")
                    .apiKey("sk-sp-0cff19f8aab542768144f3f66c781a8e")
                    .model("kimi-k2.5")
                    .temperature(0.3f);
            
            opts.systemPrompt(
                "你是FeatClaw的任务调度中心。分析用户请求，决定需要哪些Agent参与，并协调它们的工作。\n" +
                "可用Agent:\n" +
                "- project-analyzer: 项目分析专家，擅长分析项目结构和代码\n" +
                "- code-generator: 代码生成专家，擅长生成Java代码\n" +
                "- command-executor: 命令执行专家，擅长执行Maven、Git命令\n" +
                "- research-assistant: 技术调研助手，擅长搜索技术文档\n\n" +
                "你可以使用 agent_coordinator 工具来协调这些Agent完成复杂任务。"
            );
            
            opts.enableMemory();
        });
    }
    
    /**
     * 直接调用特定Agent
     * 格式: @agentName: task
     */
    private static void handleDirectAgentCall(String input) {
        int colonIndex = input.indexOf(':');
        if (colonIndex == -1) {
            System.out.println("\n❌ 格式错误。请使用: @agent-name: task\n");
            return;
        }
        
        String agentName = input.substring(1, colonIndex).trim();
        String task = input.substring(colonIndex + 1).trim();
        
        if (task.isEmpty()) {
            System.out.println("\n❌ 任务不能为空\n");
            return;
        }
        
        // 检查Agent是否存在
        if (!agentRegistry.hasAgent(agentName)) {
            System.out.println("\n❌ 未知的Agent: " + agentName);
            System.out.println("可用Agent: " + String.join(", ", agentRegistry.getAgentNames()) + "\n");
            return;
        }
        
        System.out.println("\n🤖 直接调用Agent: " + agentName);
        System.out.println("📝 任务: " + task + "\n");
        System.out.println("思考中...\n");
        
        try {
            CompletableFuture<String> future = coordinator.execute(agentName, task);
            String response = future.get();
            
            System.out.println("============================================================\n");
            System.out.println(response);
            System.out.println("\n============================================================");
            
        } catch (Exception e) {
            System.err.println("\n❌ 调用失败: " + e.getMessage() + "\n");
        }
    }
    
    /**
     * 使用调度器Agent处理用户请求
     */
    private static void processWithOrchestrator(String userInput) {
        // 添加用户消息到历史
        conversationHistory.add(Message.ofUser(userInput));
        
        // 构建系统提示，包含可用的Agent信息
        StringBuilder systemPrompt = new StringBuilder();
        systemPrompt.append("你是FeatClaw的任务调度中心。分析用户请求，决定如何完成任务。\n\n");
        systemPrompt.append("当前请求: ").append(userInput).append("\n\n");
        systemPrompt.append("可用Agent:\n");
        agentRegistry.getAllAgents().forEach(agent -> {
            if (!agent.isOrchestrator()) {
                systemPrompt.append("- ").append(agent.getName())
                           .append(" (").append(agent.getDisplayName()).append("): ")
                           .append(agent.getDescription()).append("\n");
            }
        });
        systemPrompt.append("\n你可以使用 agent_coordinator 工具协调这些Agent完成复杂任务。");
        
        // 显示处理中提示
        System.out.println("\n思考中...\n");

        try {
            // 构建消息列表
            List<Message> messages = new ArrayList<>();
            messages.add(Message.ofSystem(systemPrompt.toString()));
            messages.addAll(conversationHistory);
            
            // 执行调度器Agent
            CompletableFuture<String> future = orchestratorAgent.execute(messages);
            String response = future.get();

            // 添加助手回复到历史
            conversationHistory.add(Message.ofAssistant(response));

            // 显示响应
            System.out.println("============================================================\n");
            System.out.println(response);
            System.out.println("\n============================================================");

        } catch (Exception e) {
            System.err.println("\n❌ 处理请求时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
