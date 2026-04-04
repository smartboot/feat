---
name: feat-docs-writing
description: Feat 文档写作执行规范。编写或修改 `pages/src/content/docs` 下文档时调用。提供写作工作流、代码示例规范、质量检查清单。
---

# Feat 文档写作规范

## 定位说明

本技能聚焦**写作执行**层面，与 `feat-docs-editor` 形成互补：

| 技能 | 职责 | 触发场景 |
|------|------|----------|
| **feat-docs-editor** | 规划、审核、体系化 | 文档规划、质量审核 |
| **feat-docs-writing** | 写作执行、代码处理 | 具体编写文档内容 |

---

## 写作工作流

### 第一步：准备

```
□ 确定文档类型（见下表）
□ 搜索现有文档，确认无重复
□ 找到可用代码示例
```

| 读者需求 | 文档类型 | 标识词 |
|----------|----------|--------|
| 学习使用 | 教程 | 入门、快速开始 |
| 解决问题 | 操作指南 | 如何、配置、实现 |
| 理解原理 | 概念解释 | 原理、架构、设计 |
| 查阅 API | 参考文档 | API、参数、方法 |

### 第二步：写作

```
□ 按模板组织结构
□ 从实际项目复制代码
□ 添加中文注释
□ 使用链接引用已有内容
```

### 第三步：检查

```
□ 代码可运行
□ JDK 8 兼容
□ 链接有效
□ 无重复内容
```

---

## 代码示例规范

### 来源要求

**必须来自真实项目**，禁止手写：

| 来源 | 路径 |
|------|------|
| demo 模块 | `demo/` |
| 测试模块 | `feat-test/` |

### JDK 8 兼容性

```java
// 禁止
var list = new ArrayList<String>();           // JDK 10+
String json = """{"name":"test"}""";          // JDK 13+
record User(String name) {}                    // JDK 14+

// 正确
List<String> list = new ArrayList<String>();
String json = "{\"name\":\"test\"}";
public class User { private String name; }
```

### 注释规范

```java
HttpServer server = new HttpServer();    // 1. 创建服务器
server.httpHandler(request -> {          // 2. 设置处理器
    request.getResponse().write("Hi");   // 3. 写入响应
});
server.listen(8080);                     // 4. 启动服务
```

### 异常处理

```java
// 禁止
catch (Exception e) {
    e.printStackTrace();  // 禁止打印堆栈
}

// 正确
catch (Exception e) {
    System.err.println("处理失败: " + e.getMessage());
}
```

---

## 文档模板

### 教程

```mdx
---
title: {功能}入门
description: {一句话描述}
---

import { Aside } from '@astrojs/starlight/components'

完成本教程后，你将能够：
- 目标 1

<Aside type="caution">
开始前请确保已安装 JDK 8+。
</Aside>

## 快速开始

{步骤说明}

## 完整代码

```java
// 包含 main 方法的完整代码
```

## 验证

运行后访问 http://localhost:8080 验证。
```

### 操作指南

```mdx
---
title: 如何{完成任务}
description: {一句话描述}
---

import { Aside } from '@astrojs/starlight/components'

{问题描述}

## 实现

```java
// 代码示例
```

<Aside type="caution">{注意事项}</Aside>
```

### 概念解释

```mdx
---
title: {概念名称}
description: {一句话描述}
---

{类比或实例引入}

## 设计背景

{设计原因}

## 实现原理

{代码片段解释}
```

### 参考文档

```mdx
---
title: {API}参考
description: {一句话描述}
---

## 方法列表

| 方法 | 说明 |
|------|------|
| `method()` | 说明 |

## 详细说明

### methodName(param)

**参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| param | String | 是 | 说明 |

**返回值：** 类型 - 说明
```

---

## 内容去重规则

### 核心原则

**每个知识点只在一处详细讲解，其他地方链接引用。**

### 引用格式

```mdx
<!-- 前置知识 -->
开始前请先阅读 [快速入门](/feat/server/getstart/)。

<!-- 相关概念 -->
关于异步处理，请参考 [异步响应](/feat/server/async/)。

<!-- API 参考 -->
完整参数请参考 [ServerOptions](/feat/server/serveroptions/)。
```

### 检查方法

```
1. 搜索 pages/src/content/docs/ 目录
2. 使用关键词查找
3. 发现重复 → 改为链接引用
```

---

## 格式速查

### 标题层级

```
# H1 - 文档标题（自动生成）
## H2 - 主要章节
### H3 - 子章节
```

### Aside 组件

```mdx
<Aside type="tip">提示</Aside>
<Aside type="caution">注意</Aside>
<Aside type="danger">警告</Aside>
<Aside type="note">备注</Aside>
```

### 代码块

````mdx
```java
// Java 代码
```

```bash
# 命令行
```

```yaml
# 配置文件
```
````

### 链接

```mdx
[内部文档](/feat/cloud/controller/)
[外部链接](https://example.com)
[源码链接](https://gitee.com/xxx)
```

---

## 质量检查清单

### 写作前

- [ ] 确定文档类型
- [ ] 搜索确认无重复
- [ ] 找到可用代码示例

### 写作中

- [ ] 代码来自真实项目
- [ ] 代码 JDK 8 兼容
- [ ] 关键步骤有注释
- [ ] 使用链接引用已有内容

### 写作后

- [ ] 代码可运行
- [ ] 链接全部有效
- [ ] 标题层级正确
- [ ] Aside 使用正确

---

## 常见问题

### 代码无法运行

**原因**：手写代码未验证

**解决**：从 demo/ 或 feat-test/ 复制真实代码

### 内容重复

**原因**：未检查现有文档

**解决**：搜索后改为链接引用

### 示例不合适

**原因**：未考虑读者水平

**解决**：
- 教程：最简可运行示例
- 操作指南：典型场景示例
- 概念解释：代码片段即可

---

## 核心原则

1. **代码真实可运行** - 从实际项目复制
2. **内容不重复** - 链接引用已有内容
3. **JDK 8 兼容** - 禁止新特性
4. **读者导向** - 站在用户角度写作
