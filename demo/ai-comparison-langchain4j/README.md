# LangChain4j 示例模块

本模块展示了 LangChain4j 框架的基本使用方法。

## 特点

- **功能丰富，生态系统完善**
- **支持多种模型和工具集成**
- **提供内存、RAG、Agent 等高级功能**
- **基于 Builder 模式的 API 设计**

## 示例代码

### 基础对话

```java
ChatLanguageModel model = OpenAiChatModel.builder()
    .apiKey(System.getenv("OPENAI_API_KEY"))
    .modelName("gpt-4o")
    .build();

String response = model.generate("你好，请介绍一下 LangChain4j 的特点。");
```

### 流式输出

```java
StreamingChatLanguageModel streamingModel = OpenAiStreamingChatModel.builder()
    .apiKey(System.getenv("OPENAI_API_KEY"))
    .modelName("gpt-4o")
    .build();

streamingModel.generate("问题", new StreamingResponseHandler<AiMessage>() {
    @Override
    public void onNext(String token) {
        System.out.print(token);
    }
    
    @Override
    public void onComplete(Response<AiMessage> response) {
        System.out.println("完成");
    }
});
```

### 多轮对话

```java
List<ChatMessage> messages = new ArrayList<>();
messages.add(SystemMessage.from("系统提示"));
messages.add(UserMessage.from("问题1"));

Response<AiMessage> rsp1 = model.generate(messages);
messages.add(rsp1.content()); // 手动添加回复
messages.add(UserMessage.from("问题2"));
Response<AiMessage> rsp2 = model.generate(messages);
```

## 打包运行

```bash
# 打包 fat-jar
mvn clean package

# 运行
java -jar target/ai-comparison-langchain4j-2.0.0.jar
```

## 依赖大小

打包后可在 `target/` 目录查看 fat-jar 大小，用于与其他框架对比。
