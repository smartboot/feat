# Spring AI 示例模块

本模块展示了 Spring AI 框架的基本使用方法。

## 特点

- **与 Spring 生态深度集成**
- **支持函数调用、RAG、Prompt 模板等**
- **基于 Spring 的编程模型**
- **支持响应式编程（Reactor）**

## 示例代码

### 基础对话

```java
OpenAiApi openAiApi = new OpenAiApi(System.getenv("OPENAI_API_KEY"));

ChatClient chatClient = OpenAiChatClient.builder()
    .openAiApi(openAiApi)
    .defaultOptions(OpenAiChatOptions.builder()
        .withModel("gpt-4o")
        .build())
    .build();

String response = chatClient.prompt()
    .system("你是一个乐于助人的助手。")
    .user("你好，请介绍一下 Spring AI 的特点。")
    .call()
    .content();
```

### 流式输出

```java
ChatModel chatModel = new OpenAiChatModel(openAiApi);
Prompt prompt = new Prompt("问题");

Flux<ChatResponse> stream = chatModel.stream(prompt);
stream.subscribe(
    response -> System.out.print(response.getResult().getOutput().getContent()),
    error -> {},
    () -> System.out.println("完成")
);
```

### 多轮对话

```java
List<Message> messages = new ArrayList<>();
messages.add(new SystemMessage("系统提示"));
messages.add(new UserMessage("问题1"));

ChatResponse rsp1 = chatModel.call(new Prompt(messages));
messages.add(rsp1.getResult().getOutput()); // 手动添加回复
messages.add(new UserMessage("问题2"));
ChatResponse rsp2 = chatModel.call(new Prompt(messages));
```

## 打包运行

```bash
# 打包 fat-jar
mvn clean package

# 运行
java -jar target/ai-comparison-springai-2.0.0.jar
```

## 依赖大小

打包后可在 `target/` 目录查看 fat-jar 大小，用于与其他框架对比。
