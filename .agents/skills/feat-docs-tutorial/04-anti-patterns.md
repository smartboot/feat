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

### 反模式 D03：文档孤岛

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

## 反模式速查表

| 反模式 | 问题 | 解决策略 |
|--------|------|----------|
| C01 代码不完整 | AI 无法运行不完整代码 | 提供完整代码+依赖+验证 |
| C02 无验证步骤 | 无法确认代码正确性 | 每个示例后加验证步骤 |
| C03 缺少错误处理 | 代码健壮性不足 | 包含异常处理示例 |
| D01 信息不完整 | AI 无法推断关键信息 | 提供 What/Why/How/Where |
| D02 隐式上下文 | 前置知识不明确 | 显式声明依赖和前置知识 |
| D03 文档孤岛 | 缺乏知识关联 | 建立前后文档链接 |
