# 图表标准与组件库

## 设计原则

### 何时使用图表

```mermaid
flowchart TD
    A[考虑添加图表] --> B{内容类型?}
    B -->|流程/步骤| C[使用流程图]
    B -->|关系/层级| D[使用关系图]
    B -->|时间/阶段| E[使用用户旅程]
    B -->|分类/维度| F[使用思维导图]
    B -->|对比/状态| G[使用表格]
    
    C --> H{复杂度?}
    D --> H
    E --> H
    F --> H
    
    H -->|简单| I[使用 Mermaid]
    H -->|复杂/需交互| J[使用 HTML/SVG]
    
    G --> K[Markdown 表格]
```

### 图表 vs 文字的选择

| 使用图表 | 使用文字 |
|---------|---------|
| 展示流程步骤 | 详细说明每个步骤 |
| 展示关系连接 | 解释关系含义 |
| 展示层级结构 | 描述层级逻辑 |
| 展示状态转换 | 说明转换条件 |
| 多维度对比 | 单一维度说明 |

**原则**：图表辅助理解，不替代文字说明。

---

## Mermaid 图表组件

### 1. 流程图（Flowchart）

**适用场景**：步骤流程、决策分支、工作流

**基础模板**：

```mermaid
flowchart TD
    A[开始] --> B{判断条件}
    B -->|是| C[执行操作]
    B -->|否| D[其他操作]
    C --> E[结束]
    D --> E
```

**进阶样式**：

```mermaid
flowchart TD
    subgraph 输入
        A[用户输入]
    end
    
    subgraph 处理
        B[验证数据]
        C[处理请求]
    end
    
    subgraph 输出
        D[返回结果]
    end
    
    A --> B
    B -->|有效| C
    B -->|无效| E[错误提示]
    C --> D
    E --> A
    
    style A fill:#e3f2fd
    style D fill:#e8f5e9
    style E fill:#ffebee
```

**使用建议**：
- 节点数控制在 10 个以内
- 使用子图（subgraph）组织相关节点
- 用颜色区分不同类型节点
- 箭头标注清晰的条件

---

### 2. 关系图（Graph）

**适用场景**：知识图谱、依赖关系、模块结构

**基础模板**：

```mermaid
graph TD
    A[核心概念] --> B[子概念1]
    A --> C[子概念2]
    B --> D[细节1]
    C --> D
```

**进阶样式**：

```mermaid
graph TD
    A[快速入门] --> B[路由配置]
    A --> C[数据交互]
    A --> D[部署运维]
    
    B --> B1[基础路由]
    B --> B2[路径参数]
    
    C --> C1[JSON处理]
    C --> C2[数据库集成]
    
    D --> D1[Docker部署]
    
    style A fill:#e1f5fe
    style B fill:#fff3e0
    style C fill:#fff3e0
    style D fill:#fff3e0
```

**使用建议**：
- 使用方向控制（TD/LR/RL/BT）优化布局
- 用颜色区分层级或类型
- 避免交叉线，调整节点位置

---

### 3. 用户旅程（Journey）

**适用场景**：学习路径、用户体验、情感曲线

**基础模板**：

```mermaid
journey
    title 用户学习路径
    section 入门阶段
      阅读快速入门: 5: 用户
      完成第一个Demo: 4: 用户
    section 进阶阶段
      学习路由配置: 3: 用户
      遇到配置问题: 2: 用户
      解决问题: 5: 用户
```

**使用建议**：
- 评分 1-5，5 为最佳体验
- 标注角色（用户/系统/AI）
- 展示情感波动

---

### 4. 思维导图（Mindmap）

**适用场景**：知识分类、维度展开、检查清单

**基础模板**：

```mermaid
mindmap
  root((核心主题))
    分支1
      子分支1
      子分支2
    分支2
      子分支3
      子分支4
```

**进阶样式**：

```mermaid
mindmap
  root((优化方向))
    叙事
      增强故事性
      添加场景细节
    连贯
      添加前后文引用
      添加过渡句
    个性
      删除模板化表达
      添加独特洞察
```

**使用建议**：
- 层级不超过 3 层
- 每个节点文字简洁
- 用于展示分类而非流程

---

### 5. 时序图（Sequence）

**适用场景**：交互流程、API 调用、组件通信

**基础模板**：

```mermaid
sequenceDiagram
    participant 用户
    participant 前端
    participant 后端
    participant 数据库
    
    用户->>前端: 提交表单
    前端->>后端: POST /api/order
    后端->>数据库: 保存订单
    数据库-->>后端: 返回订单ID
    后端-->>前端: 返回成功
    前端-->>用户: 显示成功页面
```

**使用建议**：
- 参与者控制在 5 个以内
- 使用激活框（+/-）表示处理中
- 标注关键消息

---

### 6. 状态图（StateDiagram）

**适用场景**：状态机、生命周期、订单状态

**基础模板**：

```mermaid
stateDiagram-v2
    [*] --> 待支付
    待支付 --> 已支付: 支付成功
    待支付 --> 已取消: 超时取消
    已支付 --> 已发货: 商家发货
    已发货 --> 已完成: 确认收货
    已完成 --> [*]
    已取消 --> [*]
```

**使用建议**：
- 状态名简洁明确
- 标注触发条件
- 展示终态和初态

---

### 7. 甘特图（Gantt）

**适用场景**：项目计划、学习进度、里程碑

**基础模板**：

```mermaid
gantt
    title Taco Cloud 开发计划
    dateFormat  YYYY-MM-DD
    section 第1周
    项目启动           :done, a1, 2024-01-01, 2d
    模型设计           :active, a2, after a1, 3d
    section 第2周
    API开发            :a3, after a2, 5d
    前端集成           :a4, after a3, 2d
```

**使用建议**：
- 使用状态标记（done/active/crit）
- 合理设置时间粒度
- 标注关键里程碑

---

## 图表决策树

```mermaid
flowchart TD
    A[需要图表?] --> B{展示什么?}
    
    B -->|步骤/流程| C[流程图]
    B -->|关系/连接| D[关系图]
    B -->|时间/体验| E[用户旅程]
    B -->|分类/维度| F[思维导图]
    B -->|交互/通信| G[时序图]
    B -->|状态/转换| H[状态图]
    B -->|计划/进度| I[甘特图]
    B -->|对比/数据| J[表格]
    
    C --> K{复杂度?}
    D --> K
    E --> K
    F --> K
    G --> K
    H --> K
    I --> K
    
    K -->|简单| L[Mermaid]
    K -->|复杂| M[HTML/SVG]
    
    J --> N[Markdown表格]
    
    style L fill:#e8f5e9
    style M fill:#fff3e0
    style N fill:#e3f2fd
```

---

## 使用规范

### 代码块格式

```mdx
```mermaid
flowchart TD
    A[开始] --> B[结束]
```
```

### 图表标题

每个图表前应有说明文字：

```mdx
以下是 xxx 的流程：

```mermaid
flowchart TD
    ...
```

**图 1**：xxx 流程图
```

### 图表大小控制

- 流程图：节点数 ≤ 15
- 关系图：节点数 ≤ 20
- 时序图：参与者 ≤ 5
- 思维导图：层级 ≤ 3

### 颜色使用

```mermaid
%%{init: {'theme': 'base', 'themeVariables': { 'primaryColor': '#e3f2fd', 'edgeLabelBackground':'#ffffff'}}}%%
flowchart TD
    A[开始] --> B[结束]
```

**推荐配色**：
- 主节点：`#e3f2fd`（浅蓝）
- 成功/完成：`#e8f5e9`（浅绿）
- 警告/错误：`#ffebee`（浅红）
- 普通节点：`#fff3e0`（浅橙）

---

## 常见场景速查

| 场景 | 推荐图表 | 示例 |
|------|---------|------|
| 写作流程 | 流程图 | 见本文档开头 |
| 知识图谱 | 关系图 | 见 00-writing-philosophy.md |
| 学习路径 | 用户旅程 | 见 06-quality-checklist.md |
| 优化维度 | 思维导图 | 见 06-quality-checklist.md |
| 章节规划 | 流程图 | 见 07-in-action-style.md |
| API 调用 | 时序图 | 见本文档时序图示例 |
| 订单状态 | 状态图 | 见本文档状态图示例 |
| 开发计划 | 甘特图 | 见本文档甘特图示例 |

---

## 与 feat-illustrator 协作

### 何时调用 feat-illustrator

```mermaid
flowchart TD
    A[需要图表] --> B{Mermaid 能否表达?}
    B -->|能| C[使用 Mermaid]
    B -->|不能| D{类型?}
    
    D -->|架构图| E[调用 feat-illustrator]
    D -->|概念图| E
    D -->|复杂流程| E
    D -->|需要自定义样式| E
    
    C --> F[嵌入文档]
    E --> G[生成 SVG/PNG]
    G --> F
```

### 调用方式

```
使用 Skill 工具，传入 name: "feat-illustrator"
参数：
- type: "architecture" | "concept" | "flow"
- description: "详细描述"
- style: "minimalist" | "detailed"
```

### 协作流程

1. 完成文字内容
2. 标记需要插图的位置
3. 判断 Mermaid 是否足够
4. 如不够，调用 feat-illustrator
5. 在文档中引用图片（添加 alt 文本）
