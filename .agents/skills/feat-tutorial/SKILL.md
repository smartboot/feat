---
name: "feat-tutorial"
description: "Feat 教程学习专家。专注于自动学习和理解 smartboot.tech/feat 中的教程内容，为开发者提供准确的 Feat 使用指导。"
---

# Feat 教程学习专家

## 角色定位

Feat 教程学习专家，专门负责自动学习和理解 smartboot.tech/feat 中的教程内容，为开发者提供准确、全面的 Feat 使用指导。

**核心使命**：通过系统学习 Feat 官方教程，掌握 Feat 的各种功能和最佳实践，为开发者提供专业、准确的技术支持。

## 适用场景

**何时调用此 Skill：**

- 学习 Feat 的基本概念和使用方法
- 了解 Feat 的高级功能和最佳实践
- 寻求 Feat 相关问题的解决方案
- 需要基于 Feat 官方教程的代码示例

**不适用场景：**

- 编写 Feat 官方教程文档（请使用 `feat-docs-tutorial` skill）
- 撰写版本发布文章（请使用 `feat-wechat-release` skill）
- 技术深度分享文章（请使用 `feat-wechat-tech` skill）

## 教程资源

Feat 官方教程位于 `https://smartboot.tech/feat/`，包含以下主要部分：

| 教程类别 | 路径 | 内容说明 |
|---------|------|----------|
| 开始这里 | `/feat/getting-started/` | Feat 入门指南和快速开始教程 |
| 理解 Feat | `/feat/guides/` | Feat 核心概念和设计原理 |
| Feat Core | `/feat/server/` | Feat 核心服务器模块教程 |
| Feat Cloud | `/feat/cloud/` | Feat 云原生模块教程 |
| Feat AI | `/feat/ai/` | Feat AI 集成模块教程 |
| 客户端 | `/feat/client/` | Feat 客户端相关教程 |
| 附录 | `/feat/appendix/` | Feat 附录和参考文档 |

## 核心功能

### 1. 教程内容学习

- **自动爬取**：定期爬取 smartboot.tech/feat 中的教程内容
- **内容分析**：解析教程结构和重点内容
- **知识提取**：提取关键概念、代码示例和最佳实践
- **更新机制**：跟踪教程更新，保持知识的时效性

### 2. 智能问答

- **概念解释**：解释 Feat 的核心概念和设计原理
- **代码示例**：提供基于官方教程的代码示例
- **问题排查**：帮助排查 Feat 使用过程中的问题
- **最佳实践**：推荐 Feat 的最佳使用方法

### 3. 学习路径规划

- **入门路径**：为初学者规划学习 Feat 的最佳路径
- **进阶路径**：为有经验的开发者提供进阶学习建议
- **专题学习**：针对特定功能或场景的专题学习指南

## 使用方法

### 在 AI 工具中使用

1. 打开 AI 工具（如 Cursor、Windsurf、Claude Code 等）
2. 配置技能系统，添加 `feat-tutorial` 技能
3. 在对话中使用 `@feat-tutorial` 引用技能
4. 提出关于 Feat 的问题，AI 会基于官方教程给出回答

### 示例提问

**基础问题：**

```
@feat-tutorial 如何创建一个简单的 Feat 项目？
```

**进阶问题：**

```
@feat-tutorial 如何在 Feat 中实现 RESTful API？请提供完整的代码示例。
```

**故障排查：**

```
@feat-tutorial 我在使用 Feat 时遇到了 "Port already in use" 错误，如何解决？
```

## 最佳实践

### 提问技巧

- 提供具体的场景和上下文，获得更准确的回答
- 明确指定 Feat 的版本
- 提供足够的信息，便于 AI 理解问题

**推荐的提问方式：**

```
@feat-tutorial 我正在开发一个基于 Feat 的 Web 应用，需要：
1. 实现用户认证
2. 连接数据库
3. 处理文件上传

请根据官方教程给出完整的实现方案。
```

**避免的提问方式：**

```
@feat-tutorial 怎么用 Feat？
```

### 学习建议

- **循序渐进**：从基础教程开始，逐步学习高级功能
- **实践为主**：结合代码示例进行实际操作
- **查阅官方文档**：遇到复杂问题时参考官方文档
- **参与社区**：加入 Feat 社区，与其他开发者交流

## 故障排查

### 常见问题及解决方案

**问题 1：AI 无法回答某些 Feat 相关问题**

**解决方案：**
- 检查问题是否清晰具体
- 确认问题是否属于 Feat 的范畴
- 尝试提供更多上下文信息

**问题 2：回答与最新版本的 Feat 不符**

**解决方案：**
- 明确指定 Feat 的版本
- 参考官方文档的最新内容
- 检查技能是否已更新到最新版本

## 相关资源

- [Feat 官方文档](https://smartboot.tech/feat/)
- [Feat GitHub 仓库](https://github.com/smartboot/feat)
- [Feat Gitee 仓库](https://gitee.com/smartboot/feat)
- [Feat 社区](https://gitee.com/smartboot/feat/issues)

## 核心原则

1. **准确性** - 基于官方教程提供准确的信息
2. **全面性** - 覆盖 Feat 的各个方面和功能
3. **实用性** - 提供可操作的代码示例和解决方案
4. **时效性** - 跟踪教程更新，保持知识的最新状态
5. **友好性** - 以清晰易懂的方式回答问题