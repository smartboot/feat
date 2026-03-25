# FeatClaw 多Agent配置说明

FeatClaw 使用外部配置文件管理Agent和Skill，支持用户自定义修改。

## 配置目录

```
~/.featclaw/
├── agents/          # Agent配置目录 (*.yaml)
└── skills/          # Skill配置目录 (*/skill.yaml)
```

## Agent配置

### Agent配置文件位置

`~/.featclaw/agents/{agent-name}.yaml`

### Agent配置格式（YAML）

```yaml
name: agent-name                    # 唯一标识名（必填）
displayName: 显示名称              # 显示名称
description: Agent描述             # 描述
role: 角色定义                     # 角色定位
systemPrompt: |                    # 系统提示词
  提示词内容...
  支持多行
skills:                            # 支持的技能列表
  - skill-1
  - skill-2
tools:                             # 可用工具列表
  - tool-1
  - tool-2
model:                             # 模型配置
  temperature: 0.7                 # 温度参数
  maxIterations: 20                # 最大迭代次数
  modelName: model-id              # 模型名称（可选）
memory:                            # 记忆配置
  enabled: true                    # 是否启用
  topK: 5                          # 检索数量
isOrchestrator: false              # 是否为调度器
extra:                             # 额外配置（可选）
  customKey: customValue
```

### 内置Agent

| Agent名称 | 显示名称 | 描述 | 配置文件 |
|-----------|---------|------|---------|
| project-analyzer | 项目分析专家 | 分析项目结构、依赖关系和代码统计 | project-analyzer.yaml |
| code-generator | 代码生成专家 | 根据需求生成高质量Java代码 | code-generator.yaml |
| command-executor | 命令执行专家 | 安全执行Maven、Git等开发命令 | command-executor.yaml |
| research-assistant | 技术调研助手 | 搜索技术文档、解决方案 | research-assistant.yaml |
| orchestrator | 任务调度员 | 协调多个Agent协同完成复杂任务 | orchestrator.yaml |

### Agent配置示例

#### project-analyzer.yaml

```yaml
name: project-analyzer
displayName: 项目分析专家
description: 专门分析项目结构、依赖关系和代码统计的Agent
role: 你是一个专业的项目分析师，擅长分析Java/Maven项目的结构、依赖关系和代码组织
systemPrompt: |
  你擅长项目结构分析、依赖分析和代码统计。
  使用 project_analyzer 工具来分析项目。
skills:
  - project-analysis
  - dependency-analysis
  - code-statistics
tools:
  - project_analyzer
model:
  temperature: 0.3
  maxIterations: 10
memory:
  enabled: true
  topK: 3
```

#### orchestrator.yaml

```yaml
name: orchestrator
displayName: 任务调度员
description: 负责协调多个Agent协同完成复杂任务
role: 你是一个任务调度员，负责分析用户需求并协调多个专业Agent协同完成任务
systemPrompt: |
  你是FeatClaw的任务调度中心。分析用户请求，决定需要哪些Agent参与，并协调它们的工作。
  你可以将任务分解并分派给其他Agent。
skills:
  - task-decomposition
  - agent-coordination
  - workflow-management
tools:
  - agent_coordinator
model:
  temperature: 0.3
  maxIterations: 30
memory:
  enabled: true
  topK: 10
isOrchestrator: true
```

## Skill配置

### Skill配置位置

`~/.featclaw/skills/{skill-name}/skill.yaml`

每个Skill一个独立目录，可包含多个文件：
- `skill.yaml` - 技能配置（必需）
- `README.md` - 说明文档（可选）
- 其他资源文件

### Skill配置格式（YAML）

```yaml
name: skill-name                    # 唯一标识名（必填）
displayName: 显示名称              # 显示名称
description: 技能描述              # 描述
category: 类别                     # 类别（analysis/development/execution/research/coordination）
keywords:                          # 关键词列表
  - keyword1
  - keyword2
requiredTools:                     # 所需工具列表
  - tool1
  - tool2
prompt: |                          # 技能提示词
  提示词内容...
examples:                          # 使用示例
  - 示例1
  - 示例2
extra:                             # 额外配置（可选）
  customKey: customValue
```

### 内置Skill

| Skill名称 | 显示名称 | 类别 | 配置目录 |
|-----------|---------|------|---------|
| project-analysis | 项目分析 | analysis | project-analysis/ |
| code-generation | 代码生成 | development | code-generation/ |
| shell-execution | 命令执行 | execution | shell-execution/ |
| web-search | 网络搜索 | research | web-search/ |
| agent-coordination | Agent协调 | coordination | agent-coordination/ |

### Skill配置示例

#### project-analysis/skill.yaml

```yaml
name: project-analysis
displayName: 项目分析
description: 分析项目结构、目录组织、代码分布
category: analysis
keywords:
  - project
  - structure
  - directory
  - files
requiredTools:
  - project_analyzer
prompt: |
  你是一个项目分析专家。使用 project_analyzer 工具分析项目结构。
  支持以下操作：analyze_structure、read_pom、list_source_files、
  count_code_lines、analyze_dependencies
examples:
  - 分析当前项目结构
  - 列出所有Java源文件
  - 统计代码行数
```

## 工具列表

| 工具名称 | 说明 | 所属类 |
|---------|------|--------|
| project_analyzer | 项目分析工具 | ProjectAnalyzerTool |
| code_generator | 代码生成工具 | CodeGeneratorTool |
| shell_execute | Shell命令执行工具 | ShellExecuteTool |
| file_operation | 文件操作工具 | FileOperationTool |
| search | 网络搜索工具 | SearchTool |
| web_page_reader | 网页阅读工具 | WebPageReaderTool |
| agent_coordinator | Agent协调工具 | AgentCoordinatorTool |

## 配置管理

### 配置目录初始化

首次运行FeatClaw时，会自动：
1. 创建 `~/.featclaw/` 目录
2. 创建 `~/.featclaw/agents/` 目录
3. 创建 `~/.featclaw/skills/` 目录
4. 从resources复制默认配置到外部目录

### 配置热加载

修改配置文件后，需要重启FeatClaw才能生效。

### 重置配置

删除配置目录后重新运行：

```bash
rm -rf ~/.featclaw
```

### 查看配置信息

在FeatClaw交互界面输入：

```
🦀 FeatClaw > config
```

## 创建自定义Agent

### 步骤1：创建YAML配置文件

创建 `~/.featclaw/agents/my-agent.yaml`：

```yaml
name: my-agent
displayName: 我的Agent
description: 这是一个自定义Agent，用于演示
category: custom
role: 你是一个自定义Agent
systemPrompt: |
  你的任务是帮助用户完成自定义任务。
  请使用合适的工具来完成任务。
skills:
  - custom-skill
tools:
  - file_operation
model:
  temperature: 0.5
  maxIterations: 15
memory:
  enabled: true
  topK: 3
```

### 步骤2：重启FeatClaw

```bash
# 退出FeatClaw后重新运行
mvn exec:java -Dexec.mainClass="tech.smartboot.feat.demo.featclaw.Bootstrap"
```

### 步骤3：验证Agent

```
🦀 FeatClaw > agents
🦀 FeatClaw > @my-agent: 执行一个任务
```

## 创建自定义Skill

### 步骤1：创建Skill目录和配置

```bash
mkdir ~/.featclaw/skills/my-skill

cat > ~/.featclaw/skills/my-skill/skill.yaml << 'EOF'
name: my-skill
displayName: 我的技能
description: 这是一个自定义技能
category: custom
keywords:
  - my-keyword
  - custom
requiredTools:
  - file_operation
prompt: |
  这是一个自定义技能的提示词。
  描述如何使用这个技能。
examples:
  - 示例任务1
  - 示例任务2
EOF

cat > ~/.featclaw/skills/my-skill/README.md << 'EOF'
# 我的技能

这是一个自定义技能的说明文档。
EOF
```

### 步骤2：重启FeatClaw

### 步骤3：验证Skill

```
🦀 FeatClaw > skills
```

## 配置最佳实践

### 1. 命名规范

- Agent名称：小写字母+连字符，如 `code-generator`
- Skill名称：同上
- 文件名与name保持一致

### 2. 提示词编写

- 清晰描述Agent角色和职责
- 说明可用工具和使用方法
- 提供示例帮助AI理解

### 3. 温度参数选择

- 分析类Agent：较低温度（0.3）提高准确性
- 生成类Agent：中等温度（0.5-0.7）平衡创造性
- 调度器Agent：较低温度（0.3）提高稳定性

### 4. 工具配置

- 只配置Agent实际需要的工具
- 避免工具过多导致选择困难
- 使用有意义的工具名称

## 故障排查

### 配置未生效

1. 检查YAML语法是否正确
2. 确认文件位置是否正确
3. 重启FeatClaw

### Agent未加载

1. 检查 `~/.featclaw/agents/` 目录是否存在
2. 检查文件扩展名是否为 `.yaml`
3. 检查name字段是否唯一且非空

### Skill未加载

1. 检查 `~/.featclaw/skills/` 目录是否存在
2. 检查Skill目录内是否有 `skill.yaml` 文件
3. 检查YAML语法是否正确
