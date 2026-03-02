---
name: feat-docs-example-generator
description: Automatically generates runnable demo modules for Feat documentation. Use when the current demo examples are insufficient for documentation needs, or when creating new example code for tutorials, how-to guides, or references.
---

# Feat 文档示例生成器

自动为 Feat 文档生成可运行的示例代码模块。

## 核心原则：优先扩展，谨慎新建

**重要**：在创建新模块之前，必须先评估是否可以扩展现有模块。

### 优先策略

| 策略 | 优先级 | 适用场景 | 注意事项 |
|------|--------|----------|----------|
| **扩展现有模块** | ⭐⭐⭐ 优先 | 现有模块有类似功能，可通过添加新类/方法补充 | 必须不影响原有示例的运行 |
| **新建独立模块** | ⭐ 最后选择 | 现有模块无法扩展，或示例功能完全独立 | 仅在扩展不可行时使用 |

### 扩展现有模块的条件

满足以下**全部条件**时，选择扩展而非新建：

1. ✅ 现有模块的依赖满足新示例需求
2. ✅ 新示例与现有示例属于同一功能领域
3. ✅ 添加新示例不会破坏现有示例的独立性
4. ✅ 新示例不需要额外的外部资源（如数据库、Redis 等）

### 必须新建模块的条件

满足以下**任一条件**时，选择新建模块：

1. ❌ 新示例需要不同的依赖组合（如新增数据库驱动）
2. ❌ 新示例与现有模块功能领域完全不同
3. ❌ 新示例需要独占式的外部资源配置
4. ❌ 扩展会显著增加现有模块的复杂度

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

### 步骤 2：评估扩展可能性（关键步骤）

**必须完成此步骤**，确定是扩展现有模块还是新建模块。

#### 2.1 检索现有模块

检查 `demo/` 目录下的现有模块，评估是否可以扩展：

```
demo/
├── feat_static/        # 文件服务器、静态资源
├── helloworld/         # 基础 HelloWorld
├── helloworld_native/  # Native Image 示例
├── mybatis/            # MyBatis 数据库集成
└── redis_session/      # Redis Session 示例
```

#### 2.2 扩展可行性评估表

| 检查项 | 扩展可行 | 需新建 |
|--------|----------|--------|
| 依赖兼容性 | 现有依赖满足需求 | 需要新增依赖 |
| 功能相关性 | 与现有示例属于同一领域 | 功能完全独立 |
| 资源独立性 | 不需要独占资源 | 需要独立数据库/中间件 |
| 代码隔离性 | 新增类不影响现有示例 | 会干扰现有示例运行 |

#### 2.3 扩展实施原则

如果决定扩展，必须遵守：

1. **添加新类**：在现有模块中添加新的 Controller/Service/Handler 类
2. **独立入口**：新示例可通过不同的 URL 路径访问
3. **不修改现有代码**：保持原有示例的代码和功能不变
4. **可选配置**：新增配置项使用默认值，不影响原有行为

**扩展示例**：

```
demo/mybatis/
└── src/main/java/tech/smartboot/feat/demo/mybatis/
    ├── Bootstrap.java           # 原有启动类（不变）
    ├── controller/
    │   ├── UserController.java  # 原有 Controller（不变）
    │   └── OrderController.java # 新增：订单示例 ✅
    └── service/
        ├── UserService.java     # 原有 Service（不变）
        └── OrderService.java    # 新增：订单服务 ✅
```

### 步骤 3：创建模块目录（仅新建时执行）

```bash
mkdir -p demo/{module-name}/src/main/java/tech/smartboot/feat/demo/{module}
mkdir -p demo/{module-name}/src/main/resources
```

### 步骤 4：生成 pom.xml（仅新建时执行）

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

### 步骤 5：生成/修改代码文件

#### 新建模块时

创建 Bootstrap.java 启动类：

```java
package tech.smartboot.feat.demo.{module};

import tech.smartboot.feat.cloud.FeatCloud;
import tech.smartboot.feat.cloud.annotation.Bean;

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
@Bean
public class Bootstrap {
    
    public static void main(String[] args) {
        // 启动 Feat Cloud 应用
        FeatCloud.cloudServer().listen();
        
        System.out.println("{模块名称}示例已启动，访问 http://localhost:8080");
    }
}
```

#### 扩展模块时

在现有模块中添加新的业务类：

```java
// ✅ 正确：添加新的 Controller，不影响现有代码
@Controller("/order")  // 不同的路径前缀
public class OrderController {
    
    @RequestMapping("/list")
    public List<Order> list() {
        // 新示例的实现
    }
}
```

**扩展时的注意事项**：

1. **路径隔离**：新 Controller 使用不同的基础路径（如 `/order` vs 原有的 `/user`）
2. **不修改原有类**：保持现有 Controller/Service 的代码不变
3. **独立可运行**：新示例应能独立运行，不依赖特定数据

### 步骤 6：生成配置文件（如需要）

**feat.yaml 模板**：

```yaml
# Feat 应用配置
server:
  port: 8080
  
# 根据需要添加其他配置
```

### 步骤 7：添加业务代码

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