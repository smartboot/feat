# 内容架构模式

> **核心理念**：Feat 教程采用**功能模块驱动**的写作风格——每个教程对应 Feat 的一个具体功能模块，提供可直接运行的代码示例。
>
> 参考 Spring、Vert.x 等成熟框架的官方文档风格。

---

## 架构设计原则

### 1. 功能模块驱动（Feature-Driven）

**不是**：围绕一个虚构的完整项目展开长篇连载
**而是**：每个教程讲解 Feat 的一个具体功能模块

```mdx
# ❌ 项目驱动方式
我们正在构建一个电商系统...
（需要读者从头跟到尾）

# ✅ 功能模块驱动方式
## Router 路由模块
本文介绍 Feat 的路由配置功能...

## Interceptor 拦截器
本文介绍 Feat 的拦截器机制...

## WebSocket 支持
本文介绍 Feat 的 WebSocket 实现...
```

### 2. 即学即用（Ready-to-Run）

每个代码示例都应该是**完整可运行**的：

```java
// 完整的可运行示例
public class HelloWorld {
    public static void main(String[] args) {
        Feat.createServer()
            .get("/", ctx -> ctx.write("Hello, World!"))
            .listen(8080);
    }
}
```

### 3. 渐进深入（Progressive Depth）

从基础用法到高级特性：

1. **基础用法** - 最简单的代码（首屏可见）
2. **常见场景** - 实际开发中的典型用法
3. **高级特性** - 性能优化、边缘情况处理

---

## 文档类型

Feat 文档分为两类：

- **功能模块文档** - 讲解 Feat 的各个功能模块（Router、Interceptor、WebSocket 等）
- **集成文档** - 讲解与第三方组件的集成（Redis、MySQL、JWT 等）

### 1. 快速入门（Getting Started）

**目标**：让读者 5 分钟内跑通第一个 Demo  
**字数**：500-1000 字

**结构**：
```mdx
# 快速入门

## 环境要求
- JDK 8+
- Maven 3.6+

## 创建项目

### 1. 添加依赖

```xml
<dependency>
    <groupId>tech.smartboot.feat</groupId>
    <artifactId>feat-core</artifactId>
    <version>${version}</version>
</dependency>
```

### 2. 编写代码

```java
public class QuickStart {
    public static void main(String[] args) {
        Feat.createServer()
            .get("/", ctx -> ctx.write("Hello, Feat!"))
            .listen(8080);
    }
}
```

### 3. 运行

```bash
mvn compile exec:java -Dexec.mainClass="QuickStart"
```

访问 http://localhost:8080 查看结果。

## 下一步

- [创建 RESTful API](/docs/rest-api)
- [处理表单数据](/docs/form)
```

**关键约束**：
- 前 200 字内必须出现可运行的代码
- 步骤不超过 3 个
- 必须包含验证环节

---

### 2. 功能模块文档（Feature Module Guide）

**目标**：深入讲解 Feat 的某个功能模块
**字数**：1000-2500 字

**结构**：
```mdx
# Router 路由模块

Feat 的 Router 模块提供声明式路由配置，支持路径参数、查询参数、路由分组等功能。

## 注册路由

最简单的代码示例（首屏可见）：

```java
server.get("/hello", ctx -> {
    ctx.write("Hello!");
});
```

## 路径参数

从 URL 中提取参数：

```java
server.get("/user/:id", ctx -> {
    String userId = ctx.pathParam("id");
    ctx.write("User: " + userId);
});
```

## 查询参数

获取 URL 查询字符串：

```java
server.get("/search", ctx -> {
    String keyword = ctx.queryParam("q");
    ctx.write("Search: " + keyword);
});
```

## 路由分组

将相关路由组织在一起：

```java
server.group("/api", api -> {
    api.get("/users", ...);
    api.post("/users", ...);
});
```

## 完整示例

```java
public class RouterExample {
    public static void main(String[] args) {
        Feat.createServer()
            .get("/", ctx -> ctx.write("Home"))
            .get("/user/:id", ctx -> {
                ctx.write("User: " + ctx.pathParam("id"));
            })
            .listen(8080);
    }
}
```

> 🔗 **相关文档**：[拦截器](/docs/interceptor)、[错误处理](/docs/error-handling)
```

**关键约束**：
- "基础用法"必须在首屏（无需滚动）
- 每个代码块都应该是可运行的
- 包含一个"完整示例"章节

---

### 3. 集成文档（Integration Guide）

**目标**：讲解如何与第三方组件集成
**字数**：1500-3000 字

**结构**：
```mdx
# 集成 Redis

本文介绍如何在 Feat 中集成 Redis，实现数据缓存功能。

## 添加依赖

```xml
<dependency>
    <groupId>tech.smartboot.feat</groupId>
    <artifactId>feat-redis</artifactId>
    <version>${version}</version>
</dependency>
```

## 连接 Redis

```java
RedisClient redis = RedisClient.create("redis://localhost:6379");
```

## 基本操作

存储和读取数据：

```java
redis.set("key", "value");
String value = redis.get("key");
```

在 Web 应用中使用：

```java
server.get("/cache/:key", ctx -> {
    String key = ctx.pathParam("key");
    String value = redis.get(key);
    ctx.write(value != null ? value : "Not found");
});
```

## 配置选项

| 选项 | 默认值 | 说明 |
|------|--------|------|
| host | localhost | Redis 服务器地址 |
| port | 6379 | 端口号 |
| timeout | 5000 | 连接超时（毫秒） |

## 故障排查

**连接失败**（`Connection refused`）

通常是因为 Redis 服务未启动，请确保 Redis 服务正在运行。

## 版本兼容性

| Feat 版本 | Redis 版本 |
|-----------|------------|
| 1.0.x | 5.0+ |
| 1.1.x | 6.0+ |
```

**关键约束**：
- 必须列出版本兼容性
- 包含故障排查章节
- 提供配置选项表

---

## 内容组织原则

### 去重原则

每个知识点只在一处详细讲解，其他地方链接引用：

```mdx
<!-- 详细讲解处 -->
## 路由配置
完整讲解路由的各种用法...

<!-- 其他文档引用时 -->
关于路由配置，详见 [路由配置](/docs/router)。
```

### 渐进式展开

- 先讲最简单的用法
- 再讲常见场景
- 最后讲高级特性

### 代码优先

- 代码示例比文字说明更重要
- 每个代码块都应该是可运行的
- 复杂示例要有注释说明

---

## 写作风格

### 使用第二人称

```mdx
✅ 你可以使用 `ctx.pathParam()` 获取路径参数
❌ 用户可以使用 `ctx.pathParam()` 获取路径参数
```

### 简洁直接

```mdx
✅ 使用 `server.get()` 注册 GET 路由
❌ 在 Feat 框架中，开发者可以通过调用 Server 对象的 get 方法来注册一个处理 HTTP GET 请求的路由
```

### 使用提示框

```mdx
> 💡 **提示**：这是推荐的做法

> ⚠️ **注意**：这里有个常见的陷阱

> 🔗 **参见**：[相关文档](/docs/xxx)
```

---

## 检查清单

发布前确认：

- [ ] 所有代码示例都经过测试，可以运行
- [ ] "基础用法"在首屏可见（无需滚动）
- [ ] 包含一个完整的、可运行的示例
- [ ] 使用了第二人称（"你"）
- [ ] 链接到相关文档

### 避免的陷阱

- ❌ 代码示例不完整，无法直接运行
- ❌ 使用虚构的项目场景
- ❌ 章节之间强依赖，必须按顺序阅读
- ❌ 只有文字说明，缺少代码示例
