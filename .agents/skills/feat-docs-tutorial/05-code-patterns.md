# 代码模式库

> **核心目标**：提供标准化的代码模板，让 AI 能够生成一致、可运行的 Feat 代码。

---

## 核心模式

### Pattern-01: 基础 HTTP 服务器

**适用场景**：创建最简单的 Feat HTTP 服务器

**关键类**：`Feat`, `HttpServer`

**代码要点**：
- 使用 `Feat.createServer()` 创建服务器
- 使用 `.get()` 注册路由
- 使用 `.listen()` 启动服务

**验证步骤**：
1. 编译：`mvn compile`
2. 运行：`mvn exec:java -Dexec.mainClass="..."`
3. 测试：`curl http://localhost:8080/`

---

### Pattern-02: 路由配置

**适用场景**：多路由配置和请求分发

**关键类**：`Router`

**代码要点**：
- 创建 `Router` 实例
- 使用 `router.get()`, `router.post()` 注册路由
- 支持路径参数 `/users/:id`
- 使用 `Feat.createServer(router)` 绑定路由

**验证步骤**：
1. 测试不同路由：`curl http://localhost:8080/users`
2. 测试路径参数：`curl http://localhost:8080/users/123`

---

### Pattern-03: 拦截器

**适用场景**：请求拦截、权限验证、日志记录

**关键类**：`Interceptor`, `InterceptorChain`

**代码要点**：
- 实现 `Interceptor` 接口
- 使用 `chain.doIntercept()` 继续执行
- 使用 `ctx.getResponse().setStatus()` 中断请求

**验证步骤**：
1. 发送未授权请求，验证是否被拦截
2. 发送授权请求，验证是否通过

---

### Pattern-04: RESTful API

**适用场景**：完整的 RESTful API 开发

**关键类**：`Router`, `Context`, `Jackson`

**代码要点**：
- 使用 `ctx.getRequest().getBody()` 获取请求体
- 使用 `ctx.write()` 返回 JSON 响应
- 使用 `ctx.getResponse().setStatus()` 设置状态码
- 使用 Jackson 进行 JSON 序列化/反序列化

**验证步骤**：
1. POST 创建资源：`curl -X POST -d '{...}' ...`
2. GET 查询资源：`curl http://...`
3. PUT 更新资源：`curl -X PUT -d '{...}' ...`
4. DELETE 删除资源：`curl -X DELETE ...`

---

### Pattern-05: WebSocket 服务器

**适用场景**：实现 WebSocket 实时通信服务

**关键类**：`Router`, `WebSocketResponse`

**代码要点**：
- 使用 `router.ws()` 注册 WebSocket 端点
- 实现 `onConnect`, `onTextMessage`, `onClose` 回调
- 使用 `ConcurrentHashMap` 管理客户端连接
- 处理 `IOException` 异常

**验证步骤**：
1. 启动服务器
2. 使用 WebSocket 客户端连接测试

---

### Pattern-06: Feat AI 基础调用

**适用场景**：使用 Feat AI 模块进行 AI 模型调用

**关键类**：`FeatAI`, `ChatModel`, `Options`

**代码要点**：
- 使用 `FeatAI.chat(Options)` 创建客户端
- 配置 `apiKey`, `baseUrl`, `model`
- 使用 `chatModel.chat()` 进行同步调用
- 使用 `chatModel.chatStream()` 进行流式调用

**验证步骤**：
1. 配置 API Key
2. 运行程序
3. 预期输出：AI 的回复内容

---

### Pattern-07: Feat Cloud Controller

**适用场景**：使用 Feat Cloud 模块开发 RESTful API

**关键类**：`@Controller`, `@RequestMapping`, `FeatCloud`

**代码要点**：
- 使用 `@Controller` 标记类
- 使用 `@RequestMapping` 配置路由
- 使用 `@RequestParam` 绑定参数
- 使用 `FeatCloud.cloudServer().scan()` 扫描 Controller

**验证步骤**：
1. 启动服务器
2. 测试 API 端点

---

### Pattern-08: HTTP 客户端

**适用场景**：使用 Feat HTTP Client 进行 HTTP 请求

**关键类**：`HttpClient`, `HttpOptions`, `HttpResponse`

**代码要点**：
- 使用 `new HttpClient(HttpOptions)` 创建客户端
- 配置 `baseUrl`, `timeout`
- 使用 `.get()`, `.post()` 发送请求
- 使用 `.execute()` 同步执行，`.executeAsync()` 异步执行

**验证步骤**：
1. 运行程序
2. 检查 HTTP 请求是否成功
3. 验证响应内容

---

## 模式速查表

| 模式 | 适用场景 | 关键类 | 复杂度 |
|------|---------|--------|--------|
| Pattern-01 | 基础 HTTP 服务器 | Feat | ⭐ |
| Pattern-02 | 路由配置 | Router | ⭐⭐ |
| Pattern-03 | 拦截器 | Interceptor | ⭐⭐ |
| Pattern-04 | RESTful API | Router + Jackson | ⭐⭐⭐ |
| Pattern-05 | WebSocket | WebSocketResponse | ⭐⭐⭐ |
| Pattern-06 | Feat AI 基础 | FeatAI, ChatModel | ⭐⭐ |
| Pattern-07 | Feat Cloud | @Controller | ⭐⭐ |
| Pattern-08 | HTTP 客户端 | HttpClient | ⭐⭐ |

---

## 模式选择指南

**按功能选择**：

1. **需要 AI 功能？**
   - 基础调用 → Pattern-06

2. **需要 WebSocket？**
   - 实时通信 → Pattern-05

3. **使用注解开发？**
   - RESTful API → Pattern-07

4. **作为客户端？**
   - HTTP 请求 → Pattern-08

5. **服务端开发？**
   - 简单服务 → Pattern-01
   - 多路由 → Pattern-02
   - 需要拦截 → Pattern-03
   - 完整 RESTful → Pattern-04

---

## 版本历史

| 版本 | 日期 | 变更 |
|------|------|------|
| 3.0.0 | 2025-05 | 大幅简化，移除详细代码示例和 mermaid 决策树 |
| 2.0.0 | 2025-05 | 移除冗余的 AI 提示词，简化决策树 |
| 1.0.0 | 2025-04 | 初始版本 |
