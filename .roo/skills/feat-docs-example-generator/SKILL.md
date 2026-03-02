---
name: feat-docs-example-generator
description: Automatically generates runnable demo modules for Feat documentation. Use when the current demo examples are insufficient for documentation needs, or when creating new example code for tutorials, how-to guides, or references.
---

# Feat 文档示例生成器

自动为 Feat 文档生成可运行的示例代码模块。

## 触发条件

当以下情况发生时使用本技能：

1. `feat-docs-writing` skill 发现 `demo` 模块中缺少所需示例
2. 用户明确要求生成新的示例模块
3. 文档需要演示某个新功能，但现有代码无法满足

## 示例模块结构

每个 demo 模块遵循以下标准结构：

```
demo/
└── {module-name}/                 # 模块目录名（小写，连字符分隔）
    ├── pom.xml                    # Maven 配置
    └── src/
        └── main/
            ├── java/
            │   └── tech/smartboot/feat/demo/{module}/
            │       ├── Bootstrap.java      # 启动类（必需）
            │       └── ...                 # 其他业务类
            └── resources/
                └── feat.yaml              # Feat 配置文件（如需要）
```

## 模块命名规范

| 模块类型 | 命名示例 | 说明 |
|----------|----------|------|
| 基础示例 | `helloworld` | HelloWorld 级别的入门示例 |
| 功能集成 | `mybatis`, `redis-session` | 特定技术栈集成示例 |
| 场景演示 | `file-server`, `rest-api` | 特定应用场景示例 |
| 特性演示 | `async-demo`, `websocket-chat` | 单一特性深入演示 |

## 生成流程

### 步骤 1：需求分析

确认以下信息：

- **示例目的**：支持哪个文档？演示什么功能？
- **目标读者**：入门/中级/高级开发者
- **依赖组件**：需要哪些 Feat 模块？
- **运行环境**：是否需要外部依赖（数据库、Redis 等）？

### 步骤 2：创建模块目录

```bash
mkdir -p demo/{module-name}/src/main/java/tech/smartboot/feat/demo/{module}
mkdir -p demo/{module-name}/src/main/resources
```

### 步骤 3：生成 pom.xml

使用以下模板，根据实际需求调整：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>tech.smartboot.feat</groupId>
        <artifactId>feat-parent</artifactId>
        <version>1.5.0</version>
        <relativePath>../../pom.xml</relativePath>
    </parent>

    <artifactId>{module-name}</artifactId>
    <version>1.5.0</version>
    <packaging>jar</packaging>

    <name>Feat Demo - {Display Name}</name>
    <description>{模块描述}</description>

    <dependencies>
        <!-- Feat Cloud 核心 -->
        <dependency>
            <groupId>tech.smartboot.feat</groupId>
            <artifactId>feat-cloud-starter</artifactId>
            <version>${project.version}</version>
        </dependency>
        
        <!-- 根据需要添加其他依赖 -->
    </dependencies>
</project>
```

### 步骤 4：生成 Bootstrap.java

**标准模板**：

```java
package tech.smartboot.feat.demo.{module};

import tech.smartboot.feat.Feat;
import tech.smartboot.feat.cloud.CloudOptions;

/**
 * {模块名称}示例
 * 
 * 【文档类型】教程/操作指南/概念解释/参考
 * 【目的】演示 {功能描述}
 * 【前置条件】{需要的前置知识或环境}
 * 【验证方式】{如何运行和验证}
 * 
 * @author Feat Team
 */
public class Bootstrap {
    
    public static void main(String[] args) {
        // 1. 创建 CloudOptions 配置
        CloudOptions options = CloudOptions.create()
            .port(8080);
        
        // 2. 启动 Feat 应用
        Feat.run(options);
        
        System.out.println("{模块名称}示例已启动，访问 http://localhost:8080");
    }
}
```

### 步骤 5：生成配置文件（如需要）

**feat.yaml 模板**：

```yaml
# Feat 应用配置
server:
  port: 8080
  
# 根据需要添加其他配置
```

### 步骤 6：添加业务代码

根据示例需求，添加：

- Controller 类
- Service 类
- Entity/DTO 类
- Mapper 接口（如涉及数据库）
- 配置类

## 代码规范

### JDK 8 兼容性（强制）

**允许使用**：
- Lambda 表达式
- Stream API
- 方法引用
- 接口默认方法

**禁止使用**：
- 文本块 `"""..."""`
- `var` 关键字
- Record 类型
- 增强 switch
- 任何 JDK 9+ 特性

### 注释规范

每个公开类和方法必须有 Javadoc 注释：

```java
/**
 * 用户控制器
 * 
 * 演示基本的 RESTful API 实现
 * 
 * @author Feat Team
 */
@Controller
public class UserController {
    
    /**
     * 获取用户信息
     * 
     * @param id 用户ID
     * @return 用户信息
     */
    @RequestMapping("/user/{id}")
    public User getUser(@PathParam String id) {
        // ...
    }
}
```

### 依赖版本管理

所有版本继承自父 POM，不在子模块中指定版本号：

```xml
<!-- ✅ 正确：不指定版本 -->
<dependency>
    <groupId>tech.smartboot.feat</groupId>
    <artifactId>feat-core</artifactId>
</dependency>

<!-- ❌ 错误：指定版本 -->
<dependency>
    <groupId>tech.smartboot.feat</groupId>
    <artifactId>feat-core</artifactId>
    <version>1.5.0</version>
</dependency>
```

## 常见模块模板

### 基础 Web 应用

```
demo/web-basic/
├── pom.xml
└── src/main/java/tech/smartboot/feat/demo/webbasic/
    ├── Bootstrap.java
    └── controller/
        └── HelloController.java
```

### 数据库集成（MyBatis）

```
demo/mybatis-demo/
├── pom.xml
└── src/main/
    ├── java/tech/smartboot/feat/demo/mybatis/
    │   ├── Bootstrap.java
    │   ├── controller/
    │   │   └── UserController.java
    │   ├── service/
    │   │   └── UserService.java
    │   ├── mapper/
    │   │   └── UserMapper.java
    │   └── entity/
    │       └── User.java
    └── resources/
        ├── feat.yaml
        └── mybatis/
            ├── mybatis-config.xml
            └── ddl/
                └── schema.sql
```

### WebSocket 示例

```
demo/websocket-chat/
├── pom.xml
└── src/main/
    ├── java/tech/smartboot/feat/demo/websocket/
    │   ├── Bootstrap.java
    │   └── handler/
    │       └── ChatHandler.java
    └── resources/
        └── static/
            └── chat.html
```

## 验证清单

生成完成后，确认以下事项：

- [ ] 模块目录结构正确
- [ ] pom.xml 语法正确，依赖完整
- [ ] Bootstrap.java 包含 main 方法，可直接运行
- [ ] 代码符合 JDK 8 规范
- [ ] 类和方法有适当的注释
- [ ] 配置文件正确（如有）
- [ ] 可通过 `mvn compile` 编译通过
- [ ] 可通过 `mvn exec:java -Dexec.mainClass="...Bootstrap"` 运行

## 与 feat-docs-writing 的协作

生成完成后，通知 `feat-docs-writing` skill：

1. 提供新模块的路径和功能说明
2. 说明适用的文档类型
3. 提供运行验证方式

---

**生成流程口诀**：明需求 → 建目录 → 配POM → 写启动 → 加业务 → 验运行 → 通知文档