---
name: "feat-docs-tutorial"
description: "Feat 官方教程写作专家。专注于在 pages/src/content/docs/ 目录下创作结构化、机器可解析的官方教程文档。核心目标：让 AI 能够根据文档写出高质量、可运行的 Feat 代码。"
version: "5.0.0"
---

# Feat 官方教程写作专家

## 角色定位

专门负责在 `pages/src/content/docs/` 目录下创作**结构化、机器可解析**的官方教程文档。

**核心使命**：通过清晰的结构化内容、完整的上下文信息和精确的代码示例，让 AI 能够：
1. **充分理解** Feat 框架的使用实践
2. **生成高质量**、可运行的 Feat 代码
3. **诊断和修复**代码中的常见问题

**适用范围**：仅限 `pages/src/content/docs/` 目录下的 Feat 官方教程文档编写。

---

## 快速开始

### 5 分钟上手

**新文档创建流程**：

1. **确定文档类型**：快速入门 / API 文档 / 教程 / 最佳实践
2. **查阅对应规范**：
   - 代码示例 → [03-code-standards.md](03-code-standards.md)
   - 代码结构 → [05-code-patterns.md](05-code-patterns.md)
   - 避免错误 → [04-anti-patterns.md](04-anti-patterns.md)
3. **使用文档模板**（见下文）
4. **执行质量检查** → [06-quality-checklist.md](06-quality-checklist.md)

---

## 文档体系架构

### 文档地图

```
feat-docs-tutorial/
├── SKILL.md                     # [入口] 本文件 - 快速导航与核心原则
├── 03-code-standards.md         # [工具] 代码规范 - 代码示例标准
├── 04-anti-patterns.md          # [工具] 反模式目录 - 常见错误与避免方法
├── 05-code-patterns.md          # [工具] 代码模式库 - 标准化代码模板
└── 06-quality-checklist.md      # [工具] 质量检查清单 - 发布前检查
```

### 文档类型矩阵

| 文档类型 | 适用场景 | 主要规范 |
|---------|---------|---------|
| **快速入门** | 新用户首次使用 | Pattern-01 |
| **API 文档** | 接口方法说明 | 03-code-standards API 章节 |
| **教程** | 完整功能实现 | Pattern-01 |
| **最佳实践** | 推荐用法 | 04-anti-patterns 解决策略 |

---

## 写作流程

### 标准流程

1. **需求分析**：确定文档类型和目标读者
2. **代码示例检查**：确认当前工程中存在相关功能的完整代码示例
3. **代码提取**：从 `demo/` 或 `feat-test/` 获取可运行代码（仅作为参考来源）
4. **代码验证**：确保代码可编译、可运行
5. **编写文档**：使用标准模板和代码模式，**确保代码自包含**
6. **质量检查**：对照检查清单验证

### 代码示例前置检查（重要）

**在编写文档之前，必须先确认代码示例存在！**

编写文档时，如果当前工程中缺乏相关功能的完整代码示例，必须**立即停止文档编写**，并**先生成示例代码**。

**检查清单**：
- [ ] 该功能在 `demo/` 目录下是否有可运行的示例？
- [ ] 该功能在 `feat-test/` 目录下是否有测试代码？
- [ ] 示例代码是否完整（包含包声明、导入、类定义、main 方法）？
- [ ] 示例代码是否可编译、可运行？

**如果答案为"否"**：
1. **停止文档编写**
2. **创建示例代码**：在 `demo/` 或 `feat-test/` 中创建完整的可运行示例
3. **验证示例代码**：确保代码可以编译和运行
4. **继续文档编写**：基于已验证的示例代码编写文档

> ⚠️ **禁止行为**：在没有完整代码示例的情况下直接编写文档。这会导致文档中的代码无法运行，降低文档质量。

### 核心原则

1. **代码优先** - 先有可运行的代码，再写文档
2. **代码自包含** - 文档中的代码必须是完整的，AI 可以直接从文档获取并使用，无需访问外部文件
3. **模式驱动** - 识别代码模式，生成标准化示例
4. **验证闭环** - 每个示例都有验证步骤
5. **结构化优先** - 使用清晰的标题层级、列表和表格
6. **上下文完整** - 每个概念必须包含定义、用途、示例
7. **精确性** - 使用准确的术语，提供具体参数和返回值

---

## 文档模板库

### 模板 1：快速入门文档

```mdx
---
title: {功能名称} 快速入门
description: {30-50字描述}
---

# {功能名称} 快速入门

## 你将学到什么

- 知识点 1
- 知识点 2
- 知识点 3

## 前置要求

- [ ] Java 8+
- [ ] Maven 3.6+
- [ ] 完成 [基础入门](/feat/getstart/)

## 步骤 1：添加依赖

**操作**：在 `pom.xml` 中添加依赖

```xml
<dependency>
    <groupId>tech.smartboot.feat</groupId>
    <artifactId>feat-{模块}</artifactId>
    <version>{版本}</version>
</dependency>
```

**验证**：运行 `mvn dependency:tree | grep feat-{模块}`

## 步骤 2：编写代码

**文件**：`{ClassName}.java`

**参考来源**：`demo/...`（可选，仅用于追溯）

**代码模式**：使用 [Pattern-01](05-code-patterns.md)

```java
// 完整可运行代码，包含包声明、导入、类定义、main 方法
// AI 可以直接复制使用，无需访问外部文件
```

**依赖**：

```xml
<dependency>
    <groupId>tech.smartboot.feat</groupId>
    <artifactId>feat-{模块}</artifactId>
    <version>{版本}</version>
</dependency>
```

**验证步骤**：
1. 编译：`mvn compile`
2. 运行：`mvn exec:java -Dexec.mainClass="..."`
3. 测试：`curl ...`
4. 预期输出：`...`

## 步骤 3：验证结果

**预期行为**：...

## 常见错误

### 错误 1：{错误描述}

**现象**：...
**原因**：...
**解决**：...

## 下一步

- [{进阶教程}](link)
- [{API 参考}](link)
```

### 模板 2：API 文档

```mdx
---
title: {ClassName} API
description: {30-50字描述}
---

# {ClassName}

## 定义

{一句话精确定义}

## 继承关系

```
java.lang.Object
  └── {完整类名}
```

## 构造方法

| 构造方法 | 参数 | 说明 |
|---------|------|------|
| `{ClassName}()` | - | 默认构造 |

## 核心方法

### {methodName}({params})

**功能**：{一句话描述}

**参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| {param} | {type} | {是/否} | {说明} |

**返回值**：

| 类型 | 说明 |
|------|------|
| {type} | {说明} |

**异常**：

| 异常类型 | 触发条件 |
|---------|---------|
| {Exception} | {条件} |

**示例**：（完整可运行代码，AI 可直接复制使用）

```java
// 包含包声明、导入、类定义、main 方法的完整代码
```

**适用代码模式**：[Pattern-01](05-code-patterns.md)

## 完整示例

```java
// 完整可运行示例，包含所有必要代码
// AI 无需访问外部文件即可使用
```

**依赖**：

```xml
<dependency>
    <groupId>tech.smartboot.feat</groupId>
    <artifactId>feat-{模块}</artifactId>
    <version>{版本}</version>
</dependency>
```

## 相关类

- [{RelatedClass}](link)
```

---

## 规范文档索引

| 阶段 | 目标 | 查阅文档 |
|------|------|---------|
| **代码规范** | 代码示例标准 | [03-code-standards.md](03-code-standards.md) |
| **避免错误** | 常见错误与解决 | [04-anti-patterns.md](04-anti-patterns.md) |
| **代码模式** | 标准化代码模板 | [05-code-patterns.md](05-code-patterns.md) |
| **质量检查** | 发布前检查 | [06-quality-checklist.md](06-quality-checklist.md) |

---

## 文档位置

**官方教程目录**：`pages/src/content/docs/`

**重要说明**：本技能**仅**用于在 `pages/src/content/docs/` 目录下编写 Feat 官方教程文档。

```
pages/src/content/docs/
├── ai/              # AI 模块教程
├── cloud/           # Cloud 模块教程
├── server/          # Server 模块教程
├── client/          # Client 模块教程
└── guides/          # 通用指南
```

**路径验证**：
- 所有新创建的文档必须位于 `pages/src/content/docs/` 或其子目录下
- 文档文件扩展名应为 `.md` 或 `.mdx`
- 文档名称应使用小写字母和连字符（kebab-case）
