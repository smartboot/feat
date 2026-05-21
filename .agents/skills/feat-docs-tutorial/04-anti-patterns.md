# 反模式目录

以下写作方式必须避免，特别是那些会阻碍 AI 理解和生成代码的反模式。

---

## 代码相关反模式

### 反模式 C01：代码不完整

```mdx
// ❌ 错误：代码不完整，无法运行
## 路由配置

```java
router.get("/", ctx -> ctx.write("Hello"));
```

// ✅ 正确：完整可运行代码
## 路由配置

**文件**：`RouterConfig.java`

```java
package com.example;

import tech.smartboot.feat.Feat;
import tech.smartboot.feat.router.Router;

public class RouterConfig {
    public static void main(String[] args) {
        Router router = new Router();
        router.get("/", ctx -> ctx.write("Hello"));
        Feat.createServer(router).listen(8080);
        System.out.println("服务器启动成功");
    }
}
```

**依赖**：

```xml
<dependency>
    <groupId>tech.smartboot.feat</groupId>
    <artifactId>feat-core</artifactId>
    <version>2.0.0</version>
</dependency>
```

**验证步骤**：
1. 编译：`mvn compile`
2. 运行：`mvn exec:java -Dexec.mainClass="com.example.RouterConfig"`
3. 测试：`curl http://localhost:8080/`
4. 预期输出：`Hello`
```

**问题诊断**：
- AI 无法运行不完整的代码
- 缺少依赖信息
- 缺少验证步骤
- 无法验证代码正确性

**解决策略**：
1. 提供完整的代码文件（包声明、导入、类定义）
2. 包含依赖配置
3. 提供验证步骤
4. 给出预期输出

---

### 反模式 C02：无验证步骤

```mdx
// ❌ 错误：只有代码，没有验证
## 路由配置

```java
// 代码
```

// ✅ 正确：代码 + 验证步骤
## 路由配置

```java
// 代码
```

**验证步骤**：
1. **编译**：`mvn compile`
2. **运行**：`mvn exec:java -Dexec.mainClass="com.example.RouterConfig"`
3. **测试**：`curl http://localhost:8080/`
4. **预期输出**：`Hello`
```

**问题诊断**：
- AI 无法验证代码是否正确
- 无法判断配置是否生效
- 难以诊断问题

**解决策略**：
1. 每个代码块后提供验证步骤
2. 给出预期输出
3. 提供故障排查指南

---

### 反模式 C03：缺少错误处理

```mdx
// ❌ 错误：理想化的代码，无错误处理
```java
public void handle(Context ctx) {
    String data = ctx.getRequest().getBody();
    User user = objectMapper.readValue(data, User.class);
    userService.save(user);
    ctx.write("保存成功");
}
```

// ✅ 正确：包含错误处理
```java
public void handle(Context ctx) {
    try {
        String data = ctx.getRequest().getBody();
        if (data == null || data.isEmpty()) {
            ctx.getResponse().setStatus(400);
            ctx.write("请求体不能为空");
            return;
        }
        
        User user = objectMapper.readValue(data, User.class);
        
        // 验证
        if (user.getName() == null || user.getName().trim().isEmpty()) {
            ctx.getResponse().setStatus(400);
            ctx.write("用户名不能为空");
            return;
        }
        
        userService.save(user);
        ctx.getResponse().setStatus(201);
        ctx.write("保存成功");
    } catch (JsonProcessingException e) {
        ctx.getResponse().setStatus(400);
        ctx.write("JSON 格式错误: " + e.getMessage());
    } catch (Exception e) {
        ctx.getResponse().setStatus(500);
        ctx.write("服务器错误: " + e.getMessage());
    }
}
```
```

**问题诊断**：
- AI 生成的代码可能忽略边界情况
- 缺乏异常处理示例
- 代码健壮性不足

**解决策略**：
1. 包含错误处理示例
2. 展示边界情况处理
3. 提供异常处理最佳实践

---

### 反模式 C04：无代码模式

```mdx
// ❌ 错误：代码没有对应模式
## 路由配置

```java
// 自定义代码，无模式可循
```

// ✅ 正确：使用标准代码模式
## 路由配置

**代码模式**：使用 [Pattern-02](05-code-patterns.md)

```java
// 符合 Pattern-02 的代码
```
```

**问题诊断**：
- AI 无法识别代码模式
- 代码风格不一致
- 难以生成高质量代码

**解决策略**：
1. 识别代码模式
2. 使用标准模式模板
3. 标注模式引用

---

### 反模式 C05：缺少常见错误

```mdx
// ❌ 错误：没有常见错误说明
## 路由配置

```java
// 代码
```

// ✅ 正确：包含常见错误及解决方案
## 路由配置

```java
// 代码
```

## 常见错误

### 错误1：端口被占用

**现象**：
```
java.net.BindException: Address already in use: bind
```

**原因**：8080 端口已被其他程序占用

**解决**：
1. 更换端口：`.listen(8081)`
2. 或关闭占用 8080 端口的程序

### 错误2：类找不到

**现象**：
```
java.lang.ClassNotFoundException: tech.smartboot.feat.Feat
```

**原因**：缺少 feat-core 依赖

**解决**：
添加依赖到 pom.xml：
```xml
<dependency>
    <groupId>tech.smartboot.feat</groupId>
    <artifactId>feat-core</artifactId>
    <version>2.0.0</version>
</dependency>
```
```

**问题诊断**：
- AI 无法预见常见错误
- 用户遇到问题时无法解决
- 文档不完整

**解决策略**：
1. 列出常见错误
2. 提供现象描述
3. 分析原因
4. 给出解决方案
5. 提供代码对比

---

## 文档相关反模式

### 反模式 D01：信息不完整

```mdx
// ❌ 错误：信息不完整，AI 无法推断
## Router 路由

Router 可以处理请求。

// ✅ 正确：提供完整信息
## Router 路由组件

**定义**：Router 是 Feat 的请求分发组件，负责将 HTTP 请求映射到对应的处理器。

**类型**：类（`tech.smartboot.feat.router.Router`）

**位置**：`feat-core` 模块，`tech.smartboot.feat.router` 包

**解决的问题**：
- 没有 Router，所有请求都走同一个入口，代码会迅速膨胀为"面条代码"
- 无法清晰地组织不同 URL 路径的处理逻辑

**核心方法**：

| 方法 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| get | path: String, handler: RouterHandler | Router | 注册 GET 路由 |
```

**问题诊断**：
- AI 无法推断未显式声明的信息
- 缺乏类型、位置、依赖等关键信息
- 无法建立知识图谱

**解决策略**：
1. 每个概念必须包含 What/Why/How/When/Where
2. 提供完整的类型信息和位置信息
3. 显式声明依赖关系

---

### 反模式 D02：隐式上下文

```mdx
// ❌ 错误：依赖隐式上下文
## 使用 Redis 缓存

在开始前，你需要了解 Redis 的数据结构...

// ✅ 正确：显式声明前置知识
## 使用 Redis 缓存

**前置知识**：
- [Redis 基础](https://redis.io/documentation)
- [Feat 快速入门](/feat/getstart/)

**依赖**：

```xml
<dependency>
    <groupId>tech.smartboot.feat</groupId>
    <artifactId>feat-redis</artifactId>
    <version>2.0.0</version>
</dependency>
```

**基础用法**：

```java
// 一行代码启用缓存
```

> 想了解 Redis 配置细节？参见 [Redis 配置指南](/feat/redis-config/)。
```

**问题诊断**：
- AI 难以推断隐含的上下文
- 前置知识不明确
- 依赖关系不清晰

**解决策略**：
1. 显式声明前置知识
2. 列出所有依赖
3. 提供最小可用示例

---

### 反模式 D03：模糊描述

```mdx
// ❌ 错误：使用模糊的形容词
这是一个强大而灵活的功能，可以显著提升开发效率。

// ✅ 正确：用具体数据说话
该功能支持每秒处理 50,000 个请求，内存占用不到 10MB，相比传统方案性能提升 3 倍。
```

**问题诊断**：
- AI 无法量化"强大"、"灵活"等模糊描述
- 缺乏可验证的指标
- 无法与其他方案对比

**解决策略**：
1. 使用具体数字和指标
2. 提供可验证的性能数据
3. 进行对比分析

---

### 反模式 D04：过度抽象

```mdx
// ❌ 错误：只讲抽象概念，没有具体代码
## 依赖注入

依赖注入是一种设计模式，它实现了控制反转...

// ✅ 正确：概念 + 代码结合
## 依赖注入

**定义**：不需要自己 `new` 对象，框架自动帮你创建并注入。

**对比示例**：

\```java
// ❌ 传统方式
UserService userService = new UserService();

// ✅ 依赖注入
@Autowired
private UserService userService;
\```

**完整示例**：

\```java
// 完整可运行代码
\```
```

**问题诊断**：
- AI 难以将抽象概念映射到具体实现
- 缺乏可运行的代码示例
- 无法验证理解是否正确

**解决策略**：
1. 概念必须配合代码示例
2. 提供对比（正确 vs 错误）
3. 给出完整可运行示例

---

### 反模式 D05：版本漂移

```mdx
// ❌ 错误：不标注版本，或版本信息散落在各处
## 快速开始

添加依赖...

// ✅ 正确：版本信息集中展示
## 快速开始

**适用版本**：Feat ≥ 2.0.0

<Aside type="note">
Feat 1.x 用户请参考 [旧版文档](/feat/v1/getstart/)
</Aside>

**依赖**：

```xml
<dependency>
    <groupId>tech.smartboot.feat</groupId>
    <artifactId>feat-core</artifactId>
    <version>2.0.0</version>
</dependency>
```
```

**问题诊断**：
- AI 无法确定代码适用的版本
- 版本信息不一致
- 难以维护

**解决策略**：
1. 文档开头明确标注版本
2. 代码示例中包含版本信息
3. 提供版本兼容性表格

---

### 反模式 D06：信息过载

```mdx
// ❌ 错误：一次性展示所有配置选项
\```java
// 包含 50+ 行配置代码
\```

// ✅ 正确：分步骤展示，每次只讲一个概念
## 基础配置

\```java
// 最小可用配置（5 行）
\```

## 添加缓存

\```java
// 在基础配置上增加缓存（+3 行）
\```

## 添加安全

\```java
// 在缓存配置上增加安全（+5 行）
\```
```

**问题诊断**：
- AI 难以从大量信息中提取关键点
- 上下文窗口限制
- 难以建立层次关系

**解决策略**：
1. 分步骤展示
2. 每个步骤聚焦一个概念
3. 使用渐进式展开

---

### 反模式 D07：缺乏上下文

```mdx
// ❌ 错误：孤立的代码片段
\```java
userService.save(user);
\```

// ✅ 正确：提供完整上下文
\```java
// 在 UserController.java 中
@PostMapping("/users")
public ResponseEntity<?> createUser(@RequestBody User user) {
    // 保存用户到数据库
    userService.save(user);
    return ResponseEntity.ok("创建成功");
}
\```
```

**问题诊断**：
- AI 无法理解代码的上下文
- 难以推断代码的作用
- 无法验证代码的正确性

**解决策略**：
1. 提供完整的类和方法上下文
2. 说明代码在整体中的位置
3. 包含必要的导入和依赖

---

### 反模式 D08：文档孤岛

```mdx
// ❌ 错误：文档之间毫无关联
// 文档 A：快速入门
# 快速入门
...

// 文档 B：路由配置
# 路由配置
本文介绍路由配置...

// 文档 C：拦截器
# 拦截器
本文介绍拦截器...

// ✅ 正确：建立知识网络
// 文档 A：快速入门
# 快速入门
...

## 下一步
- [路由配置详解](/feat/router/) - 为你的服务添加路由

---

# 路由配置详解

**前置知识**：[快速入门](/feat/getstart/)

**后续学习**：[拦截器](/feat/interceptor/)

## 概述

在[快速入门](/feat/getstart/)中，我们创建了一个简单的服务。
现在，让我们为它添加路由功能...

## 实现路由配置
...

## 下一步

配置好路由后，你可能需要：
- 学习[拦截器](/feat/interceptor/)实现权限控制
- 了解[参数验证](/feat/validation/)确保数据安全
```

**问题诊断**：
- AI 无法建立文档间的关系
- 知识点之间没有递进关系
- 缺乏整体学习路径的引导

**解决策略**：
1. 每篇文档声明前置知识
2. 在文档开头说明学习路径位置
3. 引用之前学过的内容
4. 在结尾给出明确的下一步指引
5. 建立文档间的知识图谱

---

### 反模式 D09：缺乏场景引入

```mdx
// ❌ 错误：直接进入功能说明
# 路由配置

## 功能概述

Router 是 Feat 的请求分发组件...

## 基础用法

### 添加路由

```java
router.get("/", ctx -> ctx.write("Hello"));
```

// ✅ 正确：先建立场景
# 路由配置详解

**前置知识**：[快速入门](/feat/getstart/)

## 场景：为什么需要路由

假设你正在开发一个用户管理系统...

## 挑战：没有路由会怎样

如果没有路由，所有请求都走同一个入口...

## 解决：Feat 的路由方案

Feat 的 Router 组件...

## 实现：配置你的第一个路由

```java
// 场景：用户管理系统的路由配置
router.get("/users", ctx -> ctx.write("用户列表"));
router.post("/users", ctx -> ctx.write("创建用户"));
```
```

**问题诊断**：
- AI 不知道这个功能解决什么问题
- 缺乏上下文，难以理解代码示例
- 学完不知道在什么场景下使用

**解决策略**：
1. 开头 100 字内建立场景
2. 说明"没有这个功能会怎样"
3. 展示真实的使用场景
4. 代码示例包含场景注释

---

### 反模式 D10：非结构化内容

```mdx
// ❌ 错误：非结构化内容，难以解析
Feat 的路由功能很强大，你可以用它来做很多事情。
比如处理 GET 请求，还有 POST 请求。
配置也很简单，只需要几行代码。

// ✅ 正确：结构化内容
## Router 功能

### 支持的请求方法

| 方法 | 说明 | 示例 |
|------|------|------|
| GET | 获取资源 | `router.get("/users", handler)` |
| POST | 创建资源 | `router.post("/users", handler)` |

### 配置示例

```java
// 基础配置
Router router = new Router();
router.get("/users", ctx -> ctx.write("用户列表"));
```
```

**问题诊断**：
- AI 难以从非结构化文本中提取信息
- 缺乏明确的层次关系
- 难以建立知识图谱

**解决策略**：
1. 使用标题层级组织内容
2. 使用表格展示结构化数据
3. 使用列表展示要点
4. 使用代码块展示代码

---

### 反模式 D11：术语不一致

```mdx
// ❌ 错误：同一概念使用不同术语
// 文档 A
Router 负责分发请求...

// 文档 B
路由组件处理 HTTP 请求...

// 文档 C
请求分发器将请求映射到处理器...

// ✅ 正确：术语一致
// 所有文档统一使用"Router"
Router 是 Feat 的请求分发组件...
```

**问题诊断**：
- AI 无法识别同一概念的不同叫法
- 知识图谱中出现重复节点
- 搜索和关联困难

**解决策略**：
1. 建立术语表
2. 统一使用标准术语
3. 在首次出现时标注别名
