# SEO 与可发现性

## 标题优化

### 文档标题（frontmatter）

- 包含核心关键词，不超过 30 字
- 格式：`核心功能 + 动作/价值`
- 示例：
  - ❌ `feat-core HTTP 客户端快速入门`
  - ✅ `使用 Feat HTTP 客户端发送请求`

### 描述（description）

- 150 字以内
- 包含"如何"、"使用"、"配置"等动作词
- 示例：`本文介绍如何在 Feat 中配置 Redis 缓存，包括单节点和集群模式。`

## 关键词布局

### 正文关键词密度

- 核心关键词（如"Feat Router"）出现 3-5 次
- 长尾关键词（如"Feat 路由配置教程"）出现 1-2 次
- 自然融入，禁止堆砌

## 内部链接策略

### 链接规则

- 每个文档至少包含 2 个内部链接
- 链接文本使用描述性词语，避免"点击这里"
  - ❌ `详见[这里](/feat/xxx/)`
  - ✅ `详见[Router 配置指南](/feat/xxx/)`
- 相关文档之间互相引用，形成知识网络

## URL 规范

```
https://smartboot.tech/feat/模块/功能/

示例：
- /feat/server/router/        # Server 模块 Router 教程
- /feat/cloud/controller/     # Cloud 模块 Controller 教程
- /feat/ai/chat/              # AI 模块 Chat 教程
```

## 版本与兼容性

### 版本标注规范

在文档开头明确标注：

```mdx
---
title: 功能名称
description: 功能描述
---

<Aside type="note">
**适用版本：** Feat ≥ 3.2.0

- Feat 3.2.0+：支持全部功能
- Feat 3.1.x：不支持 XXX 功能，替代方案为 YYY
- Feat 3.0.x：不兼容，需升级
</Aside>
```

### 版本差异处理

**小版本差异：**

```mdx
<Aside type="note">
**版本差异：**
- Feat 3.2+：使用 `newConfig()` 方法
- Feat 3.1：使用 `config()` 方法（已废弃但可用）
</Aside>
```

**大版本不兼容：**

- 为每个大版本维护独立文档
- 在旧版本文档顶部添加升级提示
- 提供迁移指南链接

### 废弃功能处理

```mdx
<Aside type="caution">
**废弃警告：**
该方法在 Feat 3.3 中已废弃，将于 4.0 移除。

**替代方案：**
\```java
// 旧方式（废弃）
oldMethod();

// 新方式
newMethod();
\```
</Aside>
```
