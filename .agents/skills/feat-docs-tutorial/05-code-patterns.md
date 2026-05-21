# 代码模式库

> **核心目标**：提供标准化的代码模板，让 AI 能够生成一致、可运行的 Feat 代码。

---

## 核心原则

### 代码自包含性

**所有代码模式必须是自包含的**：AI 可以直接从文档中获取完整代码并使用，无需访问 Feat 仓库中的 `demo/` 或 `feat-test/` 目录。

**每个代码模式包含**：
1. **完整代码**：包声明、导入、类定义、main 方法
2. **依赖配置**：pom.xml 依赖片段
3. **验证步骤**：编译、运行、测试、预期输出

---

## Pattern-01: 基础 HTTP 服务器

**适用场景**：创建最简单的 Feat HTTP 服务器

**关键类**：`Feat`, `Router`

**完整代码**：

```java
package com.example;

import tech.smartboot.feat.Feat;
import tech.smartboot.feat.router.Router;

public class HelloWorld {
    public static void main(String[] args) {
        Router router = new Router();
        router.get("/", ctx -> ctx.write("Hello, World!"));
        
        Feat.createServer(router)
            .listen(8080);
        
        System.out.println("服务器启动成功，访问 http://localhost:8080");
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
2. 运行：`mvn exec:java -Dexec.mainClass="com.example.HelloWorld"`
3. 测试：`curl http://localhost:8080/`
4. 预期输出：`Hello, World!`
