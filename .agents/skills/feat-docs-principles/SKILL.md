---
name: "feat-docs-principles"
description: "Feat 框架文档编写原则与规范。定义 Feat 技术文档的写作风格、结构规范、代码示例标准，确保文档一致性和高质量。当需要编写或审查 Feat 文档时调用。"
---

# Feat 文档编写原则

## 角色定位

Feat 文档编写规范专家，负责确保所有 Feat 技术文档保持一致的风格、清晰的结构和高质量的代码示例。

**核心使命**：通过统一的文档规范，让开发者能够快速理解 Feat 的功能，降低学习成本，提升开发体验。

## 适用场景

**何时调用此 Skill：**

- 编写新的 Feat 技术文档
- 审查现有文档的规范性
- 统一文档风格和术语
- 创建文档模板

**不适用场景：**

- 微信公众号文章（请使用 `feat-wechat-tech` 或 `feat-wechat-release`）
- 生成插图（请使用 `feat-illustrator`）

---

## 文档编写负面清单

### ❌ 不要引入代码仓库本地链接

**错误示例：**

```markdown
完整示例代码请参考 [AsyncHttpDemo.java](../../feat-core/src/test/java/tech/smartboot/feat/core/server/AsyncHttpDemo.java)
```

**原因：**

- 代码仓库链接不会随文档发布
- 发布后链接会失效
- 读者无法访问

**正确做法：**

- 将关键代码直接嵌入文档
- 提供可复制的完整代码块
- 如需引用，使用文字说明而非链接

---

### ❌ 不要写无意义的开篇

**错误示例：**

```markdown
本文介绍 Feat AI 的功能和使用方法。

Feat AI 是一个强大的 AI 工具，可以帮助开发者快速构建 AI 应用...
```

**原因：**

- 没有说明读者能获得什么
- 浪费读者时间
- 缺乏行动导向

**正确做法：**

```markdown
这篇指南的目标只有一个：让你在最短时间内看到模型返回的文本。
不会涉及任何进阶配置，先把链路跑通。
```

---

### ❌ 不要过度理论化

**错误示例：**

```markdown
## 什么是异步

异步编程是一种编程范式，它允许程序在等待某些操作完成时继续执行其他任务...

[长篇理论介绍，500字以上]

## 为什么需要异步

[又一段理论]
```

**原因：**

- 开发者更关心"怎么用"
- 理论堆砌增加阅读负担
- 偏离实际使用场景

**正确做法：**

- 先给可运行的代码
- 代码后简要说明原理
- 理论服务于实践

---

### ❌ 不要回避局限性和边界

**错误示例：**

```markdown
Session 是一个强大的状态管理工具，适合所有场景使用...
```

**原因：**

- 误导读者
- 导致错误的技术选型
- 失去可信度

**正确做法：**

```markdown
## 一个更重要的问题：你真的需要 Session 吗

如果你的应用已经开始走向下面这些方向：

- 前后端分离
- Token 认证
- 多端登录

那你通常应该更谨慎地使用 Session。
```

---

### ❌ 不要使用过长的代码示例

**错误示例：**

```java
public class ChatService {
    private ChatModel model;
    private Logger logger = LoggerFactory.getLogger(ChatService.class);
    private Config config;
    private ExecutorService executor;
    
    public ChatService() {
        this.config = loadConfig();
        this.executor = Executors.newFixedThreadPool(10);
        this.model = FeatAI.chatModel(opts -> opts
                .baseUrl(config.getBaseUrl())
                .model(config.getModel())
                .temperature(config.getTemperature())
                .maxTokens(config.getMaxTokens())
                .timeout(config.getTimeout())
        );
    }
    
    public void chat(String message) {
        logger.info("收到消息: {}", message);
        model.chat(message, response -> {
            logger.info("收到响应: {}", response.getContent());
            System.out.println(response.getContent());
            executor.submit(() -> {
                saveToHistory(message, response);
            });
        });
    }
    
    private Config loadConfig() { /* ... */ }
    private void saveToHistory(String msg, ChatResponse rsp) { /* ... */ }
}
```

**原因：**

- 淹没核心逻辑
- 读者难以找到重点
- 复制后无法直接运行（依赖其他类）

**正确做法：**

```java
// 核心用法：三行代码完成对话
ChatModel model = FeatAI.chatModel(opts -> opts
        .model("qwen2.5:7b"));

model.chatStream("你好", content -> System.out.print(content));
```

---

### ❌ 不要写无意义的注释

**错误示例：**

```java
// 创建模型
ChatModel model = FeatAI.chatModel(opts -> opts
        .model("qwen2.5:7b")  // 设置模型
);

// 调用聊天方法
model.chat("你好", response -> {
    // 打印响应内容
    System.out.println(response.getContent());
});
```

**原因：**

- 注释只是重复代码
- 没有提供额外信息
- 浪费阅读时间

**正确做法：**

```java
// 流式输出适合长文本，逐字显示效果更自然
model.chatStream("你好", content -> System.out.print(content));
```

---

### ❌ 不要使用无意义的标题

**错误示例：**

```markdown
## 介绍

## 说明

## 使用方法

## 注意事项
```

**原因：**

- 标题没有信息量
- 读者无法快速定位内容
- 结构混乱

**正确做法：**

```markdown
## 添加依赖

## 配置数据库连接

## 处理异步响应

## 什么时候不该用异步
```

---

### ❌ 不要在代码中使用无意义命名

**错误示例：**

```java
public void foo(String a, int b) {
    String c = a + b;
    System.out.println(c);
}
```

**原因：**

- 降低代码可读性
- 增加理解成本
- 不专业

**正确做法：**

```java
public void greet(String name, int times) {
    String message = "Hello " + name + ", count: " + times;
    System.out.println(message);
}
```

---



### ❌ 不要混用术语

**错误示例：**

```markdown
FeatAI 提供了 chatModel 方法，你可以通过 Feat AI 的 ChatModel 类来创建...
```

**原因：**

- 同一概念多种写法
- 读者困惑
- 不专业

**正确做法：**

统一使用：
- `FeatAI`（代码中的类名）
- Feat AI（模块名称）
- `ChatModel`（代码中的类名）

---

### ❌ 不要创建过宽的表格

**错误示例：**

```markdown
| 配置项 | 说明 | 类型 | 必填 | 默认值 | 示例 | 版本 |
|--------|------|------|------|--------|------|------|
```

**原因：**

- 阅读困难
- 移动端显示异常
- 信息过载

**正确做法：**

```markdown
| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `model(...)` | 模型名称 | 必填 |
| `system(...)` | 系统提示词 | `null` |
```

---

### ❌ 不要遗漏前置条件

**错误示例：**

```markdown
## 快速开始

首先，创建一个 ChatModel...
```

**原因：**

- 读者不知道是否需要准备环境
- 运行失败时困惑
- 体验差

**正确做法：**

```markdown
## 前置条件

- JDK 8+
- Maven 3.0+
- 本地 Ollama 已安装并运行

## 快速开始
```

---

### ❌ 不要使用翻译腔

**错误示例：**

```markdown
本节将会向您介绍如何使用 Feat AI 来进行对话模型的创建和配置...
```

**原因：**

- 不符合中文技术写作习惯
- 显得生硬
- 阅读不流畅

**正确做法：**

```markdown
这篇指南的目标只有一个：让你在最短时间内看到模型返回的文本。
```

---

### ❌ 不要在文档中写 TODO

**错误示例：**

```markdown
## 高级用法

TODO: 补充高级配置说明

## 性能优化

[待完善]
```

**原因：**

- 影响文档专业性
- 读者感到被敷衍
- 降低信任度

**正确做法：**

- 完成后再发布
- 或删除该章节
- 或明确说明"即将推出"

---

### ❌ 不要过度使用 Aside

**错误示例：**

```markdown
正文内容...

<Aside type="tip">
提示1
</Aside>

正文继续...

<Aside type="caution">
警告1
</Aside>

正文继续...

<Aside type="tip">
提示2
</Aside>
```

**原因：**

- 打断阅读流
- 信息过载
- 重要提示被淹没

**正确做法：**

- 只在关键位置使用
- 每页不超过 2-3 个
- 真正重要的才用 caution/danger

---

### ❌ 不要遗漏代码块的 title

**错误示例：**

```markdown
```java
// 代码内容
```
```

**原因：**

- 读者不知道这是什么文件
- 上下文缺失
- 不利于理解

**正确做法：**

```markdown
```java title="HelloController.java"
// 代码内容
```
```

---

### ❌ 不要在文档中硬编码版本号

**错误示例：**

```markdown
当前最新版本是 2.0.0，建议使用此版本...
```

**原因：**

- 版本更新后文档过时
- 维护困难
- 容易遗漏更新

**正确做法：**

```markdown
使用 `${feat.version}` 或查看最新版本...
```

---

## 术语规范

### 统一术语表

| 术语 | 正确写法 | 避免使用 |
|------|----------|----------|
| Feat | Feat | feat、FEAT |
| Feat AI | Feat AI | FeatAI、feat-ai、FeatAi |
| Feat Cloud | Feat Cloud | FeatCloud、feat-cloud |
| Feat Core | Feat Core | FeatCore、feat-core |
| Controller | Controller | controller、控制器类 |
| RequestMapping | RequestMapping | requestMapping、@requestMapping |

### 代码中的类名/方法名

- 保持原样，不翻译
- 使用正确的驼峰命名
- 注解保留 `@` 符号

---

## 文档审查清单

发布前检查以下项目：

### 内容检查

- [ ] 没有代码仓库本地链接
- [ ] 开篇明确说明文档目标
- [ ] 包含可运行的代码示例
- [ ] 说明了适用场景和局限性
- [ ] 没有 TODO 或待完善标记

### 格式检查

- [ ] Frontmatter 完整（title、description、sidebar.order）
- [ ] 代码块带 title 属性
- [ ] 表格列数不超过 4 列
- [ ] 标题层级正确（H2 > H3 > H4）

### 语言检查

- [ ] 术语使用规范（Feat、Feat AI 等）
- [ ] 无翻译腔
- [ ] 无错别字
- [ ] 代码注释有意义

---

## 示例文档模板

```markdown
---
title: 功能名称
description: 一句话说明这篇文档的核心价值
sidebar:
    order: 1
---

import { Aside } from '@astrojs/starlight/components';

这篇指南的目标：[明确说明读者能学到什么]。

## 前置条件

- JDK 8+
- Maven 3.0+
- [其他依赖]

## 添加依赖

在 `pom.xml` 中加入：

```xml title="pom.xml"
<dependency>
    <groupId>tech.smartboot.feat</groupId>
    <artifactId>feat-xxx</artifactId>
    <version>${feat.version}</version>
</dependency>
```

<Aside>
补充说明依赖关系或版本兼容性。
</Aside>

## 核心用法

### 场景一：[具体场景]

```java title="Example.java"
// 关键代码示例，带注释说明
```

### 场景二：[具体场景]

```java title="Example2.java"
// 另一个场景的示例
```

## 配置选项

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `option1(...)` | 说明 | 默认值 |
| `option2(...)` | 说明 | 默认值 |

## 注意事项

<Aside type="caution">
重要警告或常见陷阱。
</Aside>

## 进阶用法（可选）

[高级功能或最佳实践]
```

---

## 总结

Feat 文档的核心价值观：

1. **实用** - 开发者能立即用起来
2. **坦诚** - 不回避局限性和适用边界
3. **简洁** - 没有冗余，每句话都有价值
4. **一致** - 统一的风格和术语，降低认知负担

遵循这些原则，让 Feat 文档成为开发者喜欢阅读、容易理解的优质技术文档。
