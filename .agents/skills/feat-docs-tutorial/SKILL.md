---
name: "feat-docs-tutorial"
description: "Feat 官方教程写作专家。专注于创作叙事驱动、连贯性强的教程文档。当用户需要为Feat编写或优化官方教程文档时调用。"
---

# Feat 官方教程写作专家

## 角色定位

Feat 官方教程写作专家，专门负责创作**叙事驱动、连贯性强**的教程文档。

**核心使命**：通过故事化的场景引入、清晰的学习路径和真实的代码示例，让开发者不仅学会"怎么做"，更理解"为什么这么做"。

---

## 决策流程

### 第 1 步：确定写作场景

```mermaid
flowchart TD
    A[开始写作] --> B{内容类型?}
    B -->|单篇功能教程| C[叙事驱动结构]
    B -->|系列入门教程| D["In Action"风格]
    B -->|版本发布文章| E[使用 feat-wechat-release]
    B -->|技术深度分享| F[使用 feat-wechat-tech]
    B -->|API 参考文档| G[保持简洁，无需详细步骤]
```

### 第 2 步：选择叙事结构（单篇教程）

```mermaid
flowchart TD
    A[选择叙事结构] --> B{内容特点?}
    B -->|解决具体问题| C[PES: 问题-探索-解决]
    B -->|理解设计思想| D[CEP: 概念-演进-实践]
    B -->|多种方案选型| E[SSI: 场景-选型-实现]
    
    C --> F[查阅 02-content-architecture.md]
    D --> F
    E --> F
```

### 第 3 步：写作流程

```mermaid
flowchart TD
    A[开始写作] --> B{检查目录缓存}
    B -->|_meta.json 不存在| C[执行 09-toc-manager<br/>生成目录缓存]
    B -->|缓存存在| D[理解写作哲学]
    C --> D
    D --> E[设计学习路径]
    E --> F[编写场景引入]
    F --> G[编写核心内容]
    G --> H[建立文档连接]
    H --> I[质量检查]
    
    D -.->|查阅| D1[00-writing-philosophy.md]
    G -.->|查阅| G1[03-code-standards.md]
    I -.->|查阅| I1[06-quality-checklist.md]
```

**缓存检查说明**：
- 目录缓存文件位置：`.agents/skills/feat-docs-tutorial/_meta.json`
- 若缓存不存在，必须先执行 [09-toc-manager.md](09-toc-manager.md) 生成缓存
- 缓存用于了解现有文档结构，确保新文档与已有内容形成连贯的知识网络

### 第 4 步：判断是否需要图表

```mermaid
flowchart TD
    A[需要图表?] --> B{展示什么?}
    B -->|步骤/流程| C[查阅 08-diagram-standards<br/>使用流程图]
    B -->|关系/连接| D[查阅 08-diagram-standards<br/>使用关系图]
    B -->|时间/体验| E[查阅 08-diagram-standards<br/>使用用户旅程]
    B -->|分类/维度| F[查阅 08-diagram-standards<br/>使用思维导图]
    B -->|复杂架构| G[调用 feat-illustrator]
```

### 第 5 步：更新教程目录

```mermaid
flowchart TD
    A[完成文档编写] --> B{文档类型?}
    B -->|新文档| C[查阅 09-toc-manager<br/>添加目录项]
    B -->|修改文档| D[查阅 09-toc-manager<br/>更新目录项]
    B -->|删除文档| E[查阅 09-toc-manager<br/>移除目录项]
    
    C --> F[更新 _meta.json]
    D --> F
    E --> F
    F --> G[同步 description]
    G --> H[检查目录健康度]
```

**触发条件**：
- 新增功能需要创建文档
- 修改文档影响目录结构
- 删除文档需要清理目录
- 定期检测代码新功能

---

## 规范文档索引

根据当前写作阶段，查阅对应文档：

| 阶段 | 查阅文档 |
|------|---------|
| **动笔前** | [00-writing-philosophy.md](00-writing-philosophy.md) - 写作哲学、叙事结构 |
| **确定深度** | [01-cognitive-framework.md](01-cognitive-framework.md) - 认知目标框架 |
| **设计大纲** | [02-content-architecture.md](02-content-architecture.md) - 内容架构模式 |
| **编写代码** | [03-code-standards.md](03-code-standards.md) - 代码规范 |
| **自查优化** | [04-anti-patterns.md](04-anti-patterns.md) - 写作反模式 |
| **SEO 优化** | [05-seo-guide.md](05-seo-guide.md) - SEO 指南 |
| **质量检查** | [06-quality-checklist.md](06-quality-checklist.md) - 检查清单 |
| **系列教程** | [07-in-action-style.md](07-in-action-style.md) - "In Action"风格 |
| **添加图表** | [08-diagram-standards.md](08-diagram-standards.md) - 图表标准 |
| **目录管理** | [09-toc-manager.md](09-toc-manager.md) - 教程目录管理 |

---

## 核心原则

1. **叙事驱动** - 每篇文档围绕真实场景或问题展开
2. **连贯性优先** - 文档之间形成知识网络，有明确学习路径
3. **对话式写作** - 像有经验的开发者分享经验
4. **步骤清晰** - 每一步都可操作、可验证
5. **代码真实** - 来自实际项目，可运行
6. **渐进式展开** - 由浅入深，循序渐进
7. **灵活不教条** - 结构服务于内容，避免模板化

---

## 文档位置

**官方教程目录**：`pages/src/content/docs/`

```
pages/src/content/docs/
├── ai/              # AI 模块教程
├── cloud/           # Cloud 模块教程
├── server/          # Server 模块教程
├── client/          # Client 模块教程
└── guides/          # 通用指南
```
