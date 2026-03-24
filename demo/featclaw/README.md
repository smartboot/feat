# FeatClaw - 智能软件开发助手

FeatClaw 是一个基于 FeatAgent 的 AI 软件开发助手，类似于 OpenClaw，专为 Java/Maven 项目开发场景设计。

## 特性

- 🔍 **项目分析**: 自动分析项目结构、pom.xml、源代码统计
- 💻 **代码生成**: 生成 Controller、Service、Entity 等 Java 类
- 🔧 **命令执行**: 安全执行 Maven、Git 等开发命令
- 🌐 **网络搜索**: 搜索技术文档和解决方案
- 🧠 **智能记忆**: 支持多轮对话，保持上下文

## 快速开始

### 1. 构建项目

```bash
cd demo/featclaw
mvn clean install
```

### 2. 运行 FeatClaw

```bash
mvn exec:java -Dexec.mainClass="tech.smartboot.feat.demo.featclaw.Bootstrap"
```

或者先打包再运行：

```bash
mvn clean package
java -jar target/featclaw-1.5.3.jar
```

### 3. 与 FeatClaw 交互

启动后会进入交互式命令行界面，你可以：

- 输入自然语言描述你的需求
- 输入 `help` 查看帮助信息
- 输入 `clear` 清空对话历史
- 输入 `exit` 或 `quit` 退出程序

## 使用示例

### 分析项目结构

```
🦀 FeatClaw > 请分析当前项目的结构
```

### 生成代码

```
🦀 FeatClaw > 帮我生成一个 UserController，包含增删改查接口
```

### 执行 Maven 命令

```
🦀 FeatClaw > 执行 mvn clean install 构建项目
```

### 搜索技术问题

```
🦀 FeatClaw > 搜索 Feat 框架的使用文档
```

## 项目结构

```
demo/featclaw/
├── pom.xml                                    # Maven 配置文件
├── README.md                                  # 项目说明文档
└── src/main/java/tech/smartboot/feat/demo/featclaw/
    ├── Bootstrap.java                         # 启动入口类
    ├── FeatClawAgent.java                     # FeatClaw Agent 核心类
    └── tools/
        ├── CodeGeneratorTool.java             # 代码生成工具
        ├── ProjectAnalyzerTool.java           # 项目分析工具
        └── ShellExecuteTool.java              # Shell 命令执行工具
```

## 技术栈

- **Feat-AI**: AI Agent 框架
- **Feat-Cloud**: Web 应用框架
- **DeepSeek-V3**: AI 大语言模型（通过 GiteeAI）

## 配置

在 `FeatClawAgent.java` 中可以自定义配置：

```java
new FeatClawAgent(opts -> {
    // 配置 AI 模型
    opts.chatOptions()
        .model(ChatModelVendor.GiteeAI.DeepSeek_V32)
        .temperature(0.7f);
    
    // 启用/禁用特定工具
    opts.tool(new FileOperationTool());
    opts.tool(new CodeGeneratorTool());
    // ... 其他工具
    
    // 配置记忆功能
    opts.enableMemory();
});
```

## 注意事项

1. **AI 模型配置**: 默认使用 GiteeAI 的 DeepSeek-V3 模型，需要配置相应的 API Key
2. **命令安全**: ShellExecuteTool 实现了命令白名单机制，只允许执行安全的命令
3. **文件操作**: 所有文件操作限制在项目工作目录内

## 许可证

本项目遵循 AGPL-3.0 开源协议。

---

由 Feat Team 开发和维护
