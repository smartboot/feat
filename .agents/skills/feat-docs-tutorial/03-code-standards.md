# 代码规范

## 代码示例规范

### 来源要求

- 必须来自真实项目：`demo/` 或 `feat-test/`
- 禁止手写未经测试的代码
- 代码必须可编译、可运行

### 代码获取流程

1. 在 `demo/` 或 `feat-test/` 中找到相关示例
2. 复制代码到文档
3. 运行验证（如可能）
4. 添加必要的注释

### 完整代码示例结构

每个代码示例必须包含以下信息：

```mdx
**文件**：`HelloWorld.java`

**位置**：`demo/src/main/java/com/example/HelloWorld.java`

**依赖**：

```xml
<dependency>
    <groupId>tech.smartboot.feat</groupId>
    <artifactId>feat-core</artifactId>
    <version>3.2.0</version>
</dependency>
```

**代码**：

```java
package com.example;

import tech.smartboot.feat.Feat;

/**
 * Feat Hello World 示例
 * 
 * 功能：创建一个简单的 HTTP 服务器
 * 适用版本：Feat ≥ 3.2.0
 */
public class HelloWorld {
    public static void main(String[] args) {
        // 创建 Feat 服务器并监听 8080 端口
        Feat.createServer()
            .get("/", ctx -> ctx.write("Hello, World!"))
            .listen(8080);
        
        System.out.println("服务器启动成功，访问 http://localhost:8080");
    }
}
```

**验证步骤**：
1. 运行 `mvn compile exec:java -Dexec.mainClass="com.example.HelloWorld"`
2. 访问 `http://localhost:8080`
3. 预期返回 `Hello, World!`
```

### JDK 8 兼容性

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

### 注释规范

**原则**：注释应包含 AI 可解析的结构化信息。

```java
// ❌ 只说是什么
server.listen(8080);  // 监听8080端口

// ✅ 解释为什么
server.listen(8080);  // 开发环境用 8080，生产环境建议通过配置指定

// ❌ 模糊注释
// 处理请求
public void handle(Context ctx) {
    ...
}

// ✅ 结构化注释
/**
 * 处理 HTTP 请求
 * 
 * @param ctx 请求上下文，包含请求和响应信息
 * @throws IllegalArgumentException 当请求参数无效时
 * @see Context
 */
public void handle(Context ctx) {
    ...
}
```

### 异常处理

```java
// ❌ 禁止
catch (Exception e) {
    e.printStackTrace();
}

// ✅ 正确
catch (Exception e) {
    System.err.println("处理失败: " + e.getMessage());
    // 可选：记录日志或上报监控
}
```

### 代码块标注

```mdx
// ❌ 无标题代码块
\```java
code...
\```

// ✅ 有标题和路径标注
\```java title="HelloWorld.java" {5-7}
// src/main/java/com/example/HelloWorld.java
public class HelloWorld {
    public static void main(String[] args) {
        FeatCloud.cloudServer()
                .get("/", ctx -> ctx.write("Hello Feat!"))
                .listen();
    }
}
\```
```

---

## 步骤编写规范

### 使用结构化步骤

```mdx
## 步骤1：添加依赖

**操作**：在 pom.xml 中添加依赖

```xml
<dependency>
    <groupId>tech.smartboot.feat</groupId>
    <artifactId>feat-core</artifactId>
    <version>3.2.0</version>
</dependency>
```

**验证**：运行 `mvn dependency:tree | grep feat-core`，应看到依赖信息。

## 步骤2：编写代码

**操作**：创建 HelloWorld.java

```java
// 代码
```

**验证**：运行 `mvn compile`，应无编译错误。

## 步骤3：运行服务

**操作**：执行主类

```bash
mvn exec:java -Dexec.mainClass="HelloWorld"
```

**验证**：访问 `http://localhost:8080`，应返回 `Hello, World!`。
```

### 步骤编号

- 使用阿拉伯数字：1. 2. 3.
- 保持全文一致

### 每个步骤包含

1. **步骤标题**（做什么）
2. **操作说明**（为什么）
3. **操作指令**（怎么做）
4. **验证方法**（如何确认）

---

## 格式规范

### Aside 组件使用

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

### 代码块

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

### 表格

用于参数说明、对比等：

```mdx
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| host | String | 是 | 服务器地址 |
| port | int | 否 | 端口号，默认 8080 |
```

### 链接

```mdx
[内部文档](/feat/cloud/controller/)
[外部链接](https://maven.apache.org/)
[源码链接](https://gitee.com/smartboot/feat/blob/master/xxx)
```

---

## API 文档规范

### 方法文档

```mdx
### get(String path, RouterHandler handler)

**功能**：注册 GET 路由

**参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| path | String | 是 | URL 路径模式，支持路径参数如 `/users/:id` |
| handler | RouterHandler | 是 | 请求处理器，接收 Context 对象，返回 void |

**返回值**：

| 类型 | 说明 |
|------|------|
| Router | Router 实例，支持链式调用 |

**异常**：

| 异常类型 | 触发条件 |
|----------|----------|
| IllegalArgumentException | 路径格式错误或 handler 为 null |

**示例**：

```java
Router router = new Router();
router.get("/users", ctx -> ctx.write("用户列表"));
```

**相关**：
- [Router 类](link)
- [RouterHandler 接口](link)
```

### 类文档

```mdx
## Router 类

**定义**：请求路由分发器

**位置**：`tech.smartboot.feat.router.Router`

**继承关系**：

```
java.lang.Object
  └── tech.smartboot.feat.router.Router
```

**实现接口**：
- `RouterHandler`

**构造方法**：

| 构造方法 | 说明 |
|----------|------|
| `Router()` | 创建默认 Router 实例 |

**核心方法**：

| 方法 | 返回值 | 说明 |
|------|--------|------|
| `get(String, RouterHandler)` | Router | 注册 GET 路由 |
| `post(String, RouterHandler)` | Router | 注册 POST 路由 |

**使用示例**：

```java
// 示例代码
```
```

---

## 配置文档规范

### 配置项表格

```mdx
| 配置项 | 类型 | 默认值 | 必填 | 说明 |
|--------|------|--------|------|------|
| server.port | int | 8080 | 否 | 服务器监听端口 |
| server.host | String | "0.0.0.0" | 否 | 服务器绑定地址 |
```

### 配置示例

```mdx
## 基础配置

```yaml
server:
  port: 8080
```

## 完整配置

```yaml
server:
  port: 8080
  host: "0.0.0.0"
  threadPool:
    coreSize: 10
    maxSize: 100
```
```
