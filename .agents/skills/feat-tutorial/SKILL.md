---
name: "feat-tutorial"
description: "Feat 教程学习专家。专注于自动学习和理解 smartboot.tech/feat 中的教程内容，为开发者提供准确的 Feat 使用指导。"
---

# Feat 教程学习专家

## 角色定位

Feat 教程学习专家，专门负责自动学习和理解 smartboot.tech/feat 中的教程内容，为开发者提供准确、全面的 Feat 使用指导。

**核心使命**：通过系统学习 Feat 官方教程，掌握 Feat 的各种功能和最佳实践，为开发者提供专业、准确的技术支持。

**核心优势**：优先加载本地知识，确保快速响应；本地知识不存在时自动从官方文档获取并缓存，保证知识的时效性和完整性。

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

### 1. 本地知识管理

- **知识存储**：采用分层目录结构，按教程类别组织本地知识
- **知识索引**：建立高效的知识索引，支持快速检索
- **知识加载**：启动时优先加载本地知识，确保快速响应
- **知识更新**：定期检查本地知识的完整性和时效性

### 2. 官方文档获取与缓存

- **自动爬取**：定期爬取 smartboot.tech/feat 中的教程内容
- **增量更新**：只获取新增或更新的内容，提高效率
- **本地缓存**：将获取的内容缓存到本地，减少网络请求
- **错误处理**：网络故障时使用本地缓存，确保服务可用性

### 3. 智能问答

- **概念解释**：解释 Feat 的核心概念和设计原理
- **代码示例**：提供基于官方教程的代码示例
- **问题排查**：帮助排查 Feat 使用过程中的问题
- **最佳实践**：推荐 Feat 的最佳使用方法

### 4. 学习路径规划

- **入门路径**：为初学者规划学习 Feat 的最佳路径
- **进阶路径**：为有经验的开发者提供进阶学习建议
- **专题学习**：针对特定功能或场景的专题学习指南

### 5. 知识加载优先级

- **优先顺序**：本地知识 > 本地缓存 > 官方文档
- **智能切换**：本地知识不存在时自动从官方文档获取
- **缓存策略**：获取后自动缓存，下次访问直接使用本地数据
- **更新提示**：当本地知识与官方文档存在差异时提供更新提示

## 使用方法

### 在 AI 工具中使用

1. 打开 AI 工具（如 Cursor、Windsurf、Claude Code 等）
2. 配置技能系统，添加 `feat-tutorial` 技能
3. 在对话中使用 `@feat-tutorial` 引用技能
4. 提出关于 Feat 的问题，AI 会基于本地知识或官方教程给出回答

### 本地知识管理

- **知识存储位置**：技能会在本地创建 `.knowledge` 目录存储知识
- **知识更新**：首次使用时会自动从官方文档获取并缓存
- **手动更新**：可以通过特定命令触发知识更新
- **缓存清理**：可以通过特定命令清理本地缓存

### 网络依赖

- **离线使用**：首次获取后可在无网络环境下使用本地知识
- **网络恢复**：网络恢复后会自动同步官方文档的更新
- **网络错误**：网络故障时使用本地缓存，确保服务可用性

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
- 触发知识更新，确保本地知识的完整性

**问题 2：回答与最新版本的 Feat 不符**

**解决方案：**
- 明确指定 Feat 的版本
- 手动触发知识更新，同步官方文档的最新内容
- 检查本地缓存是否过期

**问题 3：本地知识加载失败**

**解决方案：**
- 检查本地 `.knowledge` 目录是否存在
- 检查目录权限是否正确
- 重新触发知识获取和缓存

**问题 4：网络错误导致无法获取官方文档**

**解决方案：**
- 检查网络连接
- 使用本地缓存的知识
- 网络恢复后自动同步

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
6. **高效性** - 优先使用本地知识，提高响应速度
7. **可靠性** - 网络故障时使用本地缓存，确保服务可用性
8. **智能性** - 自动从官方文档获取并缓存知识，减少人工干预

## 实现建议

### 本地知识管理系统

#### 存储结构

```
.agents/skills/feat-tutorial/
├── SKILL.md              # 技能配置文件
├── .knowledge/           # 本地知识存储目录
│   ├── getting-started/  # 开始这里教程
│   ├── guides/           # 理解 Feat 教程
│   ├── server/           # Feat Core 教程
│   ├── cloud/            # Feat Cloud 教程
│   ├── ai/               # Feat AI 教程
│   ├── client/           # 客户端教程
│   └── appendix/         # 附录和参考文档
└── src/                  # 技能实现代码
    ├── KnowledgeManager.java  # 知识管理类
    ├── DocumentFetcher.java   # 文档获取类
    └── PriorityLoader.java    # 优先级加载器
```

#### 核心类设计

**KnowledgeManager.java**

```java
public class KnowledgeManager {
    private static final String KNOWLEDGE_DIR = ".knowledge";
    
    // 加载本地知识
    public Map<String, String> loadLocalKnowledge(String category) {
        Map<String, String> knowledge = new HashMap<>();
        File categoryDir = new File(KNOWLEDGE_DIR, category);
        
        if (categoryDir.exists() && categoryDir.isDirectory()) {
            File[] files = categoryDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().endsWith(".md")) {
                        try {
                            String content = Files.readString(file.toPath());
                            knowledge.put(file.getName(), content);
                        } catch (IOException e) {
                            // 处理异常
                        }
                    }
                }
            }
        }
        return knowledge;
    }
    
    // 保存知识到本地
    public void saveKnowledge(String category, String fileName, String content) {
        File categoryDir = new File(KNOWLEDGE_DIR, category);
        if (!categoryDir.exists()) {
            categoryDir.mkdirs();
        }
        
        File file = new File(categoryDir, fileName);
        try {
            Files.writeString(file.toPath(), content);
        } catch (IOException e) {
            // 处理异常
        }
    }
}
```

**DocumentFetcher.java**

```java
public class DocumentFetcher {
    private static final String BASE_URL = "https://smartboot.tech/feat";
    private KnowledgeManager knowledgeManager;
    
    public DocumentFetcher(KnowledgeManager knowledgeManager) {
        this.knowledgeManager = knowledgeManager;
    }
    
    // 从官方文档获取内容
    public String fetchDocument(String path) {
        try {
            URL url = new URL(BASE_URL + path);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder content = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        content.append(line).append("\n");
                    }
                    
                    // 缓存到本地
                    String category = extractCategory(path);
                    String fileName = extractFileName(path);
                    knowledgeManager.saveKnowledge(category, fileName, content.toString());
                    
                    return content.toString();
                }
            }
        } catch (Exception e) {
            // 处理异常
        }
        return null;
    }
    
    // 提取类别和文件名
    private String extractCategory(String path) {
        // 实现路径解析逻辑
        return "general";
    }
    
    private String extractFileName(String path) {
        // 实现文件名提取逻辑
        return "index.md";
    }
}
```

**PriorityLoader.java**

```java
public class PriorityLoader {
    private KnowledgeManager knowledgeManager;
    private DocumentFetcher documentFetcher;
    
    public PriorityLoader(KnowledgeManager knowledgeManager, DocumentFetcher documentFetcher) {
        this.knowledgeManager = knowledgeManager;
        this.documentFetcher = documentFetcher;
    }
    
    // 优先加载本地知识，不存在则从官方文档获取
    public String loadContent(String category, String fileName) {
        // 1. 尝试从本地知识加载
        Map<String, String> localKnowledge = knowledgeManager.loadLocalKnowledge(category);
        if (localKnowledge.containsKey(fileName)) {
            return localKnowledge.get(fileName);
        }
        
        // 2. 本地知识不存在，从官方文档获取
        String path = buildPath(category, fileName);
        return documentFetcher.fetchDocument(path);
    }
    
    private String buildPath(String category, String fileName) {
        // 实现路径构建逻辑
        return "/" + category + "/" + fileName.replace(".md", "");
    }
}
```

### 使用流程

1. **初始化**：技能启动时初始化 KnowledgeManager、DocumentFetcher 和 PriorityLoader
2. **查询处理**：接收到用户查询时，使用 PriorityLoader 加载相关知识
3. **知识更新**：定期检查并更新本地知识，确保与官方文档同步
4. **错误处理**：网络故障时使用本地缓存，确保服务可用性

### 性能优化

- **索引优化**：为本地知识建立索引，提高检索速度
- **增量更新**：只获取和更新变化的内容，减少网络请求
- **缓存策略**：合理设置缓存过期时间，平衡时效性和性能
- **并发处理**：使用多线程处理文档获取和缓存，提高效率

### 扩展性考虑

- **模块化设计**：将知识管理、文档获取和优先级加载分离，便于扩展
- **插件机制**：支持自定义知识源和缓存策略
- **配置管理**：通过配置文件管理知识源和缓存参数
- **监控机制**：监控知识更新状态和系统性能
