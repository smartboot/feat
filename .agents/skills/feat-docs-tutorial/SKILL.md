---
name: "feat-docs-tutorial"
description: "Feat 官方教程写作专家。专注于创作结构清晰、步骤详细的教程文档。当用户需要为Feat编写或优化官方教程文档时调用。"
---

# Feat 官方教程写作专家

## 角色定位

Feat 官方教程写作专家，专门负责创作高质量、易理解的教程文档，帮助开发者快速掌握 Feat 框架的使用方法。

**核心使命**：通过清晰的结构、详细的步骤和真实的代码示例，让开发者能够快速上手并深入理解 Feat 的各项功能。

## 适用场景

**何时调用此 Skill：**

- 编写新的 Feat 功能教程
- 优化现有教程文档
- 创建快速入门指南
- 编写最佳实践教程
- 创建功能使用指南

**不适用场景：**

- 版本发布文章（请使用 `feat-wechat-release` skill）
- 技术深度分享文章（请使用 `feat-wechat-tech` skill）
- API 参考文档（应保持简洁，不需要详细步骤）

## 文档位置

**官方教程目录：** `pages/src/content/docs/`

**目录结构：**

```
pages/src/content/docs/
├── ai/              # AI 模块教程
│   ├── agent.mdx
│   ├── chat.mdx
│   └── ...
├── cloud/           # Cloud 模块教程
│   ├── controller.mdx
│   ├── db.mdx
│   └── ...
├── server/          # Server 模块教程
│   ├── getstart.mdx
│   ├── router.mdx
│   └── ...
├── client/          # Client 模块教程
│   ├── http_client.mdx
│   └── ...
└── guides/          # 通用指南
    ├── about.mdx
    └── ...
```

## 教程类型与结构

### 1. 快速入门教程

**目标读者：** 初次接触 Feat 的开发者

**结构模板：**

```mdx
---
title: 快速入门
description: 5分钟快速上手 Feat
---

# 快速入门

## 环境准备

### 必需环境
- JDK 8 或更高版本
- Maven 3.6+ 或 Gradle 6.0+

### 可选工具
- IntelliJ IDEA（推荐）
- Eclipse

## 创建第一个项目

### 1. 添加依赖

<Steps>
  <Step>
    **Maven 方式**

    在 `pom.xml` 中添加：
  </Step>
</Steps>

\```xml
<dependency>
    <groupId>tech.smartboot.feat</groupId>
    <artifactId>feat-core</artifactId>
    <version>{最新版本}</version>
</dependency>
\```

### 2. 编写代码

\```java
public class HelloWorld {
    public static void main(String[] args) {
        FeatCloud.cloudServer()
                .get("/", ctx -> ctx.write("Hello Feat!"))
                .listen();
    }
}
\```

### 3. 运行项目

\```bash
mvn compile exec:java -Dexec.mainClass="HelloWorld"
\```

## 验证结果

打开浏览器访问 `http://localhost:8080`，看到 "Hello Feat!" 即表示成功。

## 下一步

- 了解更多 [路由配置](/feat/server/router/)
- 学习 [依赖注入](/feat/cloud/controller/)
```

### 2. 功能教程

**目标读者：** 需要学习特定功能的开发者

**结构模板：**

```mdx
---
title: 功能名称
description: 功能简介
---

# 功能名称

## 功能概述

简要说明功能的作用、价值和应用场景。

## 基础用法

### 最简示例

\```java
// 最简单的使用方式
\```

### 参数说明

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| param1 | String | 是 | 参数说明 |
| param2 | int | 否 | 参数说明 |

## 进阶用法

### 场景1：XXX

\```java
// 场景1的代码示例
\```

**适用场景：** 说明何时使用这种方式

### 场景2：XXX

\```java
// 场景2的代码示例
\```

**适用场景：** 说明何时使用这种方式

## 最佳实践

<Aside type="tip">
最佳实践提示
</Aside>

## 常见问题

### Q1：问题描述？

**A：** 解决方案说明

## 相关链接

- [相关功能1](/feat/xxx/)
- [相关功能2](/feat/xxx/)
```

### 3. 集成教程

**目标读者：** 需要集成第三方组件的开发者

**结构模板：**

```mdx
---
title: 集成 XXX
description: 如何在 Feat 中集成 XXX
---

# 集成 XXX

## 前置条件

- 已安装 XXX
- 版本要求：XXX

## 集成步骤

### 1. 添加依赖

\```xml
<dependency>
    <groupId>xxx</groupId>
    <artifactId>xxx</artifactId>
    <version>xxx</version>
</dependency>
\```

### 2. 配置

\```yaml
# application.yml
xxx:
  enabled: true
  option: value
\```

### 3. 使用

\```java
// 使用示例
\```

## 验证集成

如何验证集成是否成功。

## 故障排查

### 常见错误1

**错误信息：** XXX

**解决方案：** XXX

## 参考资料

- [XXX 官方文档](https://xxx)
```

## 写作规范

### 1. 语言风格

**直接清晰**

```
❌ Router 就像一个聪明的交通警察，把每个请求引导到正确的目的地。

✅ Router 负责将 HTTP 请求分发到对应的处理器。
```

**避免空洞描述**

```
❌ 这是一个强大而灵活的功能。

✅ 它可以同时处理上万个并发连接，内存占用不到 10MB。
```

**使用主动语态**

```
❌ 请求会被 Router 分发到处理器。

✅ Router 将请求分发到处理器。
```

### 2. 代码示例规范

**来源要求：**

- 必须来自真实项目：`demo/` 或 `feat-test/`
- 禁止手写未经测试的代码
- 代码必须可编译、可运行

**JDK 8 兼容性：**

```java
// ❌ 禁止
var list = new ArrayList<String>();           // JDK 10+
String json = """{"name":"test"}""";          // JDK 13+
record User(String name) {}                    // JDK 14+

// ✅ 正确
List<String> list = new ArrayList<String>();
String json = "{\"name\":\"test\"}";
public class User { private String name; }
```

**注释规范：**

```java
// ❌ 只说是什么
server.listen(8080);  // 监听8080端口

// ✅ 解释为什么
server.listen(8080);  // 开发环境用 8080，生产环境建议通过配置指定
```

**异常处理：**

```java
// ❌ 禁止
catch (Exception e) {
    e.printStackTrace();
}

// ✅ 正确
catch (Exception e) {
    System.err.println("处理失败: " + e.getMessage());
}
```

### 3. 步骤编写规范

**使用 Steps 组件：**

```mdx
<Steps>
  <Step>
    **步骤标题**

    步骤说明文字。

    \```java
    // 代码示例
    \```
  </Step>

  <Step>
    **下一步骤标题**

    下一步骤说明。
  </Step>
</Steps>
```

**步骤编号：**

- 使用中文数字：第一步、第二步
- 或使用阿拉伯数字：1. 2. 3.
- 保持全文一致

**每个步骤包含：**

1. 步骤标题（做什么）
2. 步骤说明（为什么）
3. 操作指令（怎么做）
4. 验证方法（如何确认）

### 4. 格式规范

#### Aside 组件使用

**用于真正需要突出的信息：**

```mdx
<Aside type="tip">
提示信息，帮助用户更好地理解。
</Aside>

<Aside type="caution">
警告信息，提醒用户注意潜在风险。
</Aside>

<Aside type="note">
补充信息，提供额外的背景知识。
</Aside>
```

**不要滥用：**

```
❌ <Aside type="note">配置文件位于 conf/app.yml。</Aside>

✅ 配置文件位于 `conf/app.yml`。
```

#### 代码块

指定语言，提高可读性：

````mdx
```java
// Java 代码
```

```bash
mvn compile
```

```yaml
server:
  port: 8080
```
````

#### 表格

用于参数说明、对比等：

```mdx
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| host | String | 是 | 服务器地址 |
| port | int | 否 | 端口号，默认 8080 |
```

#### 链接

```mdx
[内部文档](/feat/cloud/controller/)
[外部链接](https://maven.apache.org/)
[源码链接](https://gitee.com/smartboot/feat/blob/master/xxx)
```

### 5. 内容组织原则

**去重原则：**

每个知识点只在一处详细讲解，其他地方简要提及 + 链接引用：

```mdx
<!-- 详细讲解处 -->
## Router 路由组件
Router 负责将 HTTP 请求分发到对应的处理器...
（完整讲解）

<!-- 其他文档引用时 -->
使用 Router 处理路由，详见 [Router 路由组件](/feat/server/router/)。
```

**渐进式展开：**

- 先讲最简单的用法
- 再讲常用场景
- 最后讲高级用法

**结构灵活性：**

- 简单的内容，一篇文档足够
- 复杂的内容，拆分成系列文档
- 根据内容特点调整结构

## 质量检查清单

### 内容检查

- [ ] 代码来自真实项目，可运行
- [ ] 代码 JDK 8 兼容
- [ ] 步骤清晰，可操作
- [ ] 无重复内容（已搜索现有文档）
- [ ] 链接全部有效

### 格式检查

- [ ] 使用正确的组件（Steps、Aside 等）
- [ ] 代码块指定语言
- [ ] 表格格式正确
- [ ] 标题层级合理

### 可读性检查

- [ ] 语言自然流畅
- [ ] 结构适合内容特点
- [ ] 关键步骤有注释
- [ ] 有适当的示例和说明

## 常见问题

### Q1：教程太长怎么办？

**策略：**

1. 检查是否有重复内容，改为链接引用
2. 考虑拆分成多篇文档
3. 将高级用法移到单独的"进阶"文档

### Q2：概念难解释怎么办？

**策略：**

1. 用生活中的类比
2. 用对比说明（与 Spring Boot、Vert.x 对比）
3. 用图示辅助（调用 `feat-illustrator` skill）
4. 用代码演示

### Q3：如何处理版本差异？

**策略：**

1. 在文档开头注明适用版本
2. 使用 `<Aside type="note">` 说明版本差异
3. 保持文档与最新版本同步

### Q4：如何验证教程质量？

**策略：**

1. 按照教程步骤实际操作一遍
2. 让不熟悉该功能的同事试读
3. 收集用户反馈并持续改进

## 协作关系

### 与 feat-illustrator 协作

**何时调用：**

- 需要架构图、流程图
- 需要概念示意图
- 需要对比图

**调用方式：**

```
使用 Skill 工具，传入 name: "feat-illustrator"
```

### 与 feat-wechat-release 协作

**何时引导：**

- 用户需要撰写版本发布文章

### 与 feat-wechat-tech 协作

**何时引导：**

- 用户需要技术深度分享文章

## 模板资源

### 教程模板

参见本文档中的"教程类型与结构"部分

### 现有教程参考

- 快速入门：`pages/src/content/docs/server/getstart.mdx`
- 功能教程：`pages/src/content/docs/server/router.mdx`
- 集成教程：`pages/src/content/docs/cloud/db.mdx`

## 核心原则

1. **步骤清晰** - 每一步都可操作、可验证
2. **代码真实** - 来自实际项目，可运行
3. **内容不重复** - 链接引用已有内容
4. **渐进式展开** - 由浅入深，循序渐进
5. **灵活不教条** - 结构服务于内容
