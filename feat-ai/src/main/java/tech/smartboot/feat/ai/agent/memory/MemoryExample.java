/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.agent.memory;

import tech.smartboot.feat.ai.agent.ReActAgent;
import tech.smartboot.feat.ai.embedding.EmbeddingModel;
import tech.smartboot.feat.ai.vector.ChromaVectorStore;
import tech.smartboot.feat.ai.vector.VectorStore;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 记忆系统使用示例
 * <p>
 * 演示如何在FeatAgent中配置和使用记忆系统。
 * </p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class MemoryExample {

    /**
     * 示例1：使用内存记忆（简单快速，适合开发和测试）
     */
    public void example1_InMemoryMemory() throws Exception {
        // 创建Agent并配置内存记忆
        ReActAgent agent = new ReActAgent(opts -> {
            // 启用内存记忆
            opts.memory(memoryOpts -> {
                memoryOpts.maxMessages(100)              // 最大存储100条消息
                        .defaultTopK(5)                    // 默认检索5条
                        .similarityThreshold(0.5)          // 相似度阈值
                        .includeThoughts()                 // 包含思考过程
                        .importanceThreshold(0.5);         // 重要性过滤
            });
            
            // 设置会话ID（可选，用于隔离不同会话的记忆）
            opts.sessionId("user-session-123");
            
            // 配置记忆检索数量
            opts.memoryTopK(3);
        });

        // 执行对话 - 会自动存储到记忆并检索相关历史
        CompletableFuture<String> result1 = agent.execute("你好，我是张三");
        System.out.println("回复1: " + result1.get());

        CompletableFuture<String> result2 = agent.execute("请问我叫什么名字？");
        System.out.println("回复2: " + result2.get());

        CompletableFuture<String> result3 = agent.execute("我喜欢Java编程");
        System.out.println("回复3: " + result3.get());

        // 关闭Agent时可以清除记忆
        // agent.options().getMemory().clear();
    }

    /**
     * 示例2：直接使用Memory API（不通过Agent）
     */
    public void example2_MemoryDirectUsage() {
        // 创建内存记忆
        Memory memory = Memory.inMemory(opts -> {
            opts.maxMessages(50)
                    .defaultTopK(10)
                    .disableSessionIsolation(); // 允许跨会话检索
        });

        // 添加记忆消息
        MemoryMessage message1 = MemoryMessage.ofUser("今天天气很好");
        message1.sessionId("session-1");
        memory.add(message1);

        MemoryMessage message2 = MemoryMessage.ofAssistant("是的，阳光明媚");
        message2.sessionId("session-1");
        memory.add(message2);

        MemoryMessage message3 = MemoryMessage.ofUser("你喜欢什么编程语言？");
        message3.sessionId("session-1");
        memory.add(message3);

        // 检索相关记忆
        List<MemoryMessage> results = memory.search("编程语言", 5);
        System.out.println("找到 " + results.size() + " 条相关记忆:");
        for (MemoryMessage msg : results) {
            System.out.println("  - " + msg.getRole().getDisplayName() + ": " + msg.getContent());
        }

        // 获取最近记忆
        List<MemoryMessage> recent = memory.getRecent(3);
        System.out.println("\n最近3条记忆:");
        for (MemoryMessage msg : recent) {
            System.out.println("  - " + msg.getRole().getDisplayName() + ": " + msg.getContent());
        }

        // 获取记忆数量
        System.out.println("\n总记忆数: " + memory.size());

        // 清除指定会话的记忆
        memory.clear("session-1");
        System.out.println("清除后记忆数: " + memory.size());
    }

    /**
     * 示例3：使用向量记忆（需要配置EmbeddingModel和VectorStore）
     */
    public void example3_VectorMemory() throws Exception {
        // 创建EmbeddingModel（需要配置API密钥等）
        EmbeddingModel embeddingModel = new EmbeddingModel();
        embeddingModel.options()
                .model("text-embedding-3-small")
                .baseUrl("https://api.example.com")
                .apiKey("your-api-key");

        // 创建VectorStore（这里使用Chroma作为示例）
        ChromaVectorStore vectorStore = VectorStore.chroma(opts -> {
            opts.setUrl("http://localhost:8000")
                    .collectionName("agent_memory");
        });

        // 创建向量记忆
        Memory memory = Memory.vector(opts -> {
            opts.vectorStore(vectorStore)
                    .embeddingModel(embeddingModel)
                    .collectionName("agent_memory")
                    .vectorDimension(1536)
                    .sessionId("user_123");  // 设置会话ID实现隔离
        });

        // 使用记忆...
        memory.add(MemoryMessage.ofUser("这是一个需要向量检索的消息"));
        
        List<MemoryMessage> results = memory.search("向量检索", 5);
        System.out.println("向量搜索结果: " + results.size() + " 条");
    }

    /**
     * 示例4：在Agent中使用向量记忆
     */
    public void example4_AgentWithVectorMemory() throws Exception {
        // 配置向量记忆（需要提前配置好EmbeddingModel和VectorStore）
        VectorMemory memory = Memory.vector(opts -> {
            // 这里假设已经有配置好的embeddingModel和vectorStore
            // opts.vectorStore(vectorStore);
            // opts.embeddingModel(embeddingModel);
            opts.collectionName("agent_conversations");
        });

        // 创建Agent并使用向量记忆
        ReActAgent agent = new ReActAgent(opts -> {
            opts.memory(memory);
            opts.sessionId("user-456");
            opts.memoryTopK(5);
        });

        // 执行多轮对话，Agent会自动使用向量记忆进行检索
        CompletableFuture<String> result = agent.execute("请帮我总结之前的对话内容");
        System.out.println(result.get());
    }

    /**
     * 示例5：禁用记忆功能
     */
    public void example5_DisableMemory() {
        ReActAgent agent = new ReActAgent(opts -> {
            // 禁用记忆功能
            opts.disableMemory();
            
            // 或者不提供任何记忆配置，默认也是禁用状态
        });

        // 此时Agent不会存储或检索任何历史记忆
    }

    /**
     * 示例6：创建不同类型的记忆消息
     */
    public void example6_MemoryMessageTypes() {
        Memory memory = Memory.inMemory(opts -> {});

        // 用户消息
        MemoryMessage userMsg = MemoryMessage.ofUser("我想查询今天的天气");
        memory.add(userMsg);

        // AI助手消息
        MemoryMessage assistantMsg = MemoryMessage.ofAssistant("今天北京天气晴朗");
        assistantMsg.importance(1.5); // 提高重要性
        memory.add(assistantMsg);

        // 系统消息
        MemoryMessage systemMsg = MemoryMessage.ofSystem("你是一个有用的助手");
        memory.add(systemMsg);

        // 工具调用结果
        MemoryMessage toolMsg = MemoryMessage.ofTool("getWeather", 
                "{\"city\": \"北京\"}", 
                "{\"temperature\": 25, \"condition\": \"晴朗\"}");
        memory.add(toolMsg);

        // 思考过程
        MemoryMessage thoughtMsg = MemoryMessage.ofThought("用户想查询天气，我需要调用天气API");
        memory.add(thoughtMsg);

        // 动作
        MemoryMessage actionMsg = MemoryMessage.ofAction("调用getWeather工具获取北京天气");
        memory.add(actionMsg);

        // 观察
        MemoryMessage observationMsg = MemoryMessage.ofObservation("获取到北京天气：25度，晴朗");
        memory.add(observationMsg);

        // 带元数据的消息
        MemoryMessage metaMsg = MemoryMessage.ofUser("重要消息");
        metaMsg.metadata("category", "important")
                .metadata("source", "user-input");
        memory.add(metaMsg);
    }

    public static void main(String[] args) throws Exception {
        MemoryExample example = new MemoryExample();
        
        System.out.println("===== 示例1：内存记忆 =====");
        // example.example1_InMemoryMemory();
        
        System.out.println("\n===== 示例2：直接使用Memory API =====");
        example.example2_MemoryDirectUsage();
        
        System.out.println("\n===== 示例6：不同类型的记忆消息 =====");
        example.example6_MemoryMessageTypes();
    }
}
