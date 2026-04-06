# FeatClaw - 多Agent协同智能开发助手

FeatClaw 是一个基于 FeatAgent 的 AI 软件开发助手，支持多Agent协同工作，专为 Java/Maven 项目开发场景设计。

## 特性

- 🔍 **项目分析**: 自动分析项目结构、pom.xml、源代码统计
- 💻 **代码生成**: 生成 Controller、Service、Entity 等 Java 类
- 🔧 **命令执行**: 安全执行 Maven、Git 等开发命令
- 🌐 **网络搜索**: 搜索技术文档和解决方案
- 🤖 **多Agent协同**: 多个专业Agent协同完成复杂任务
- 📁 **外部配置**: Agent和Skill配置存储在外部目录，便于自定义
- 📝 **YAML配置**: Agent配置使用YAML格式，清晰易读

## 配置目录结构

```
~/.featclaw/
├── agents/                          # Agent配置文件目录
│   ├── project-analyzer.yaml        # 项目分析专家
│   ├── code-generator.yaml          # 代码生成专家
│   ├── command-executor.yaml        # 命令执行专家
│   ├── research-assistant.yaml      # 技术调研助手
│   └── orchestrator.yaml            # 任务调度员
└── skills/                          # Skill配置目录
    ├── project-analysis/            # 项目分析技能
    │   ├── skill.yaml
    │   └── README.md
    ├── code-generation/             # 代码生成技能
    │   ├── skill.yaml
    │   └── README.md
    ├── shell-execution/             # 命令执行技能
    │   ├── skill.yaml
    │   └── README.md
    ├── web-search/                  # 网络搜索技能
    │   ├── skill.yaml
    │   └── README.md
    └── agent-coordination/          # Agent协调技能
        ├── skill.yaml
        └── README.md
```

## 快速开始

### 1. 构建项目

```bash
cd demo/featclaw
mvn clean install
```

### 2. 运行 FeatClaw

```bash
mvn exec:java -Dexec.mainClass="tech.smartboot.feat.demo.featclaw.Bootstrap"
```

首次运行会自动在 `~/.featclaw/` 目录创建配置并复制默认配置。

### 3. 与 FeatClaw 交互

启动后会进入交互式命令行界面：

```
============================================================
                                                            
              FeatClaw - 多Agent协同开发助手                 
                                                            
   配置文件位置: ~/.featclaw/                                
                                                            
============================================================

FeatClaw 功能:
  🔍 [分析] 项目结构、依赖关系、代码统计
  💻 [生成] Java类、Controller、Service、配置文件
  🔧 [执行] Maven、Git 等安全命令
  🌐 [搜索] 技术文档和解决方案
  🤖 [协同] 多Agent智能协作完成复杂任务

快捷命令:
  agents    - 查看Agent详情
  skills    - 查看技能列表
  config    - 查看配置信息
  help      - 显示帮助信息
  clear     - 清空对话历史
  exit      - 退出程序

直接调用Agent: @agent-name: 任务描述
```

## 使用示例

### 自动调度模式（推荐）

直接输入你的需求，调度器会自动分析并协调合适的Agent：

```
🦀 FeatClaw > 帮我生成一个UserController，包含增删改查接口
```

### 直接调用Agent

使用 `@agent-name: task` 格式直接调用指定Agent：

```
🦀 FeatClaw > @code-generator: 生成一个UserService类
🦀 FeatClaw > @project-analyzer: 分析当前项目结构
🦀 FeatClaw > @command-executor: 执行 mvn clean install
🦀 FeatClaw > @research-assistant: 搜索Feat框架文档
```

### 快捷命令

```
🦀 FeatClaw > agents    # 查看所有Agent详情
🦀 FeatClaw > skills    # 查看所有技能详情
🦀 FeatClaw > config    # 查看配置目录信息
🦀 FeatClaw > help      # 显示帮助信息
🦀 FeatClaw > clear     # 清空对话历史
🦀 FeatClaw > exit      # 退出程序
```

## Agent配置

Agent配置存储在 `~/.featclaw/agents/*.yaml`，格式如下：

```yaml
name: code-generator
displayName: 代码生成专家
description: 根据需求生成高质量Java代码的Agent
role: 你是一个专业的Java代码生成专家
systemPrompt: |
  你擅长生成Java代码。使用 code_generator 工具来生成代码文件。
  遵循Java编码规范，添加适当的注释。
skills:
  - code-generation
  - java-programming
tools:
  - code_generator
  - file_operation
model:
  temperature: 0.5
  maxIterations: 15
memory:
  enabled: true
  topK: 5
```

### 配置字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| name | string | Agent唯一标识名（必填） |
| displayName | string | 显示名称 |
| description | string | Agent描述 |
| role | string | 角色定义 |
| systemPrompt | string | 系统提示词 |
| skills | list | 支持的技能列表 |
| tools | list | 可用工具列表 |
| model.temperature | float | 模型温度参数 |
| model.maxIterations | int | 最大迭代次数 |
| memory.enabled | boolean | 是否启用记忆 |
| memory.topK | int | 记忆检索数量 |
| isOrchestrator | boolean | 是否为调度器 |

## Skill配置

Skill配置按目录隔离存放，每个Skill一个目录：

```
~/.featclaw/skills/skill-name/
├── skill.yaml    # 技能配置
└── README.md     # 说明文档
```

### skill.yaml 格式

```yaml
name: code-generation
displayName: 代码生成
description: 生成Java类、配置文件、POM片段等
category: development
keywords:
  - code
  - java
  - generate
requiredTools:
  - code_generator
  - file_operation
prompt: |
  你是一个代码生成专家。使用 code_generator 工具生成代码。
examples:
  - 生成一个UserController
  - 创建一个Service类
```

## 扩展开发

### 添加新Agent

1. 在 `~/.featclaw/agents/` 目录创建新的YAML文件：

```yaml
# ~/.featclaw/agents/my-agent.yaml
name: my-agent
displayName: 我的Agent
description: 这是一个自定义Agent
role: 你是...
systemPrompt: |
  你的任务是...
skills:
  - my-skill
tools:
  - file_operation
model:
  temperature: 0.7
  maxIterations: 20
memory:
  enabled: true
  topK: 5
```

2. 重启FeatClaw即可加载新Agent

### 添加新Skill

1. 在 `~/.featclaw/skills/` 目录创建新目录：

```bash
mkdir ~/.featclaw/skills/my-skill
cat > ~/.featclaw/skills/my-skill/skill.yaml << 'EOF'
name: my-skill
displayName: 我的技能
description: 这是一个自定义技能
category: custom
keywords:
  - my-keyword
requiredTools:
  - file_operation
prompt: 这是技能提示词
examples:
  - 示例1
  - 示例2
EOF
```

2. 重启FeatClaw即可加载新Skill

### 添加新工具

1. 创建工具类实现 `AgentTool` 接口
2. 在 `AgentCoordinator.createTool()` 方法中注册工具
3. 在Agent配置的 `tools` 列表中添加工具名
4. 在Skill配置的 `requiredTools` 中添加工具名

## 重置配置

如需恢复默认配置，可删除配置目录后重新运行：

```bash
rm -rf ~/.featclaw
# 然后重新运行 FeatClaw
```

## 技术栈

- **Feat-AI**: AI Agent 框架
- **Feat-Cloud**: Web 应用框架
- **SnakeYAML**: YAML解析库
- **DeepSeek-V3**: AI 大语言模型

## 注意事项

1. **配置文件位置**: 默认在 `~/.featclaw/`，首次运行会自动创建
2. **YAML格式**: 配置使用YAML格式，注意缩进和语法
3. **Agent名称**: Agent名称必须唯一，使用小写字母和连字符
4. **Skill目录**: Skill按目录隔离，每个Skill一个独立目录
5. **命令安全**: ShellExecuteTool 实现了命令白名单机制

## 许可证

本项目遵循 Apache-2.0 开源协议。

---

由 Feat Team 开发和维护
