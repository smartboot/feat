# Feat AI 示例模块

本模块展示了 Feat AI 框架的基本使用方法。

## 特点

- **简洁的链式 API 设计**
- **原生支持异步编程模型**
- **轻量级，无 Spring 依赖**
- **内置多种模型提供商支持**
- **自动管理对话上下文**

## 示例代码

### 基础对话

```java
ChatModel chatModel = FeatAI.chatModel(opts ->
    opts.model("gpt-4o")
       .system("你是一个乐于助人的助手。")
);

chatModel.chat("你好，请介绍一下 Feat AI 的特点。")
    .thenAccept(response -> {
        System.out.println(response.getContent());
    })
    .join();
```

### 流式输出

```java
chatModel.chatStream("问题", new ChatStreamListener() {
    @Override
    public void onStreamResponse(String content) {
        System.out.print(content);
    }
    
    @Override
    public void onCompletion(ChatResponse response) {
        System.out.println("完成");
    }
});
```

### 多轮对话

```java
// 自动保持上下文，无需手动管理
chatModel.chat("问题1").thenAccept(rsp1 -> {
    chatModel.chat("问题2").thenAccept(rsp2 -> {
        // rsp2 自动包含上文上下文
    });
});
```

## 打包运行

```bash
# 打包 fat-jar
mvn clean package

# 运行
java -jar target/ai-comparison-feat-2.0.0.jar
```

## 依赖大小

打包后可在 `target/` 目录查看 fat-jar 大小，用于与其他框架对比。
