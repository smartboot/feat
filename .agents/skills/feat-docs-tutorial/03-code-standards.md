# 代码规范

## 代码示例规范

### 来源要求

- **代码必须自包含**：文档中的代码示例必须是完整的，AI 可以直接从文档中获取并使用
- **必须优先从 `demo/` 或 `feat-test/` 获取真实代码**，禁止手写未经测试的代码
- 代码必须可编译、可运行

### 代码示例前置检查

**在获取代码之前，必须先确认代码示例存在！**

如果在 `demo/` 或 `feat-test/` 中找不到相关功能的完整代码示例，必须**立即停止文档编写**，并**先生成示例代码**。

**检查清单**：
- [ ] 该功能在 `demo/` 目录下是否有可运行的示例？
- [ ] 该功能在 `feat-test/` 目录下是否有测试代码？
- [ ] 示例代码是否完整（包含包声明、导入、类定义、main 方法）？
- [ ] 示例代码是否可编译、可运行？

**如果代码示例不存在**：
1. **停止文档编写**
2. **创建示例代码**：在 `demo/` 或 `feat-test/` 中创建完整的可运行示例
3. **验证示例代码**：确保代码可以编译和运行
4. **继续文档编写**：基于已验证的示例代码编写文档

### 完整代码示例结构

每个代码示例必须包含以下信息：

```mdx
**文件**：`HelloWorld.java`

**参考来源**：`demo/src/main/java/com/example/HelloWorld.java`（可选，仅用于追溯）

**依赖**：

```xml
<dependency>
    <groupId>tech.smartboot.feat</groupId>
    <artifactId>feat-core</artifactId>
    <version>2.0.0</version>
</dependency>
```

**代码**：（完整代码，AI 可直接复制使用）

```java
package com.example;

import tech.smartboot.feat.Feat;

public class HelloWorld {
    public static void main(String[] args) {
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
    <version>2.0.0</version>
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

### 每个步骤包含

1. **步骤标题**（做什么）
2. **操作说明**（为什么）
3. **操作指令**（怎么做）
4. **验证方法**（如何确认）

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

**示例**：（完整可运行代码，AI 可直接复制使用）

```java
Router router = new Router();
router.get("/users", ctx -> ctx.write("用户列表"));
```
```
