# Feat 项目开发规则

## 构建命令

| 命令 | 说明 |
|------|------|
| `mvn compile` | 编译项目 |
| `mvn test` | 运行所有测试 |
| `mvn test -pl feat-core` | 仅测试 feat-core 模块 |
| `mvn package -DskipTests` | 打包（跳过测试） |
| `mvn clean install` | 完整构建并安装到本地仓库 |
| `mvn exec:java -Dexec.mainClass="xxx.Bootstrap"` | 运行指定主类 |

## 代码规范

### JDK 版本

- **目标版本**：JDK 8
- **源码兼容性**：必须兼容 JDK 8

### 允许使用的特性

- Lambda 表达式
- Stream API
- 方法引用
- 接口默认方法
- Optional 类
- CompletableFuture

### 禁止使用的特性（JDK 9+）

- `var` 关键字
- 文本块 `"""`
- Record 类型
- 增强 switch 表达式
- sealed 类
- 模块系统相关 API
- 任何 JDK 9+ 新增 API

### 命名规范

| 类型 | 规范 | 示例 |
|------|------|------|
| 包名 | 全小写，连字符分隔 | `tech.smartboot.feat.core` |
| 类名 | 大驼峰 | `HttpServer`, `RouterHandler` |
| 方法名 | 小驼峰 | `handleRequest`, `parseBody` |
| 常量 | 全大写下划线 | `MAX_BUFFER_SIZE` |
| 变量 | 小驼峰 | `httpRequest`, `responseBody` |

### 注释规范

- 类注释：使用中文 Javadoc，说明类的作用
- 公共方法：必须有 Javadoc 注释
- 关键逻辑：行内注释说明意图
- 注释语言：中文

## 模块结构

```
feat-parent (根 POM)
├── feat-core          # 核心 HTTP 服务器
├── feat-ai            # AI 集成模块
├── feat-cloud         # 云原生模块
├── feat-cloud-aot     # AOT 编译支持
├── feat-cloud-aot-vm  # AOT VM 支持
├── feat-cloud-starter # 快速启动器
├── feat-test          # 测试模块
├── demo/              # 示例项目
│   ├── feat_static/   # 静态资源服务示例
│   ├── helloworld/    # 入门示例
│   ├── mybatis/       # MyBatis 集成示例
│   └── featclaw/      # AI Agent 示例
└── pages/             # 文档站点
```

## 模块依赖关系

```
feat-core (基础层)
    ↓
feat-cloud (扩展层)
    ↓
feat-cloud-starter (应用层)

feat-ai (独立模块，依赖 feat-core)
```

## 测试规范

### 测试框架

- JUnit 4.13.1

### 测试命名

- 测试类命名：`{ClassName}Test.java`
- 测试方法命名：`test{MethodName}_{Scenario}`

### 测试资源

- 测试配置文件：`src/test/resources/feat.yml`
- 测试数据：`src/test/resources/` 目录下

## 文档规范

参考 `.roo/skills/feat-docs-writing/SKILL.md`

### 文档位置

- 用户文档：`pages/src/content/docs/`
- 开发者日记：`feat/diary/`
- 版本说明：`feat/v*.md`

### 文档类型

- 教程：新手入门，引导完成项目
- 操作指南：解决具体问题
- 概念解释：阐述设计原理
- 参考文档：API 说明

## Git 提交规范

### 提交消息格式

```
<type>(<scope>): <subject>

<body>
```

### 类型（type）

| 类型 | 说明 |
|------|------|
| feat | 新功能 |
| fix | 修复 bug |
| docs | 文档更新 |
| style | 代码格式调整 |
| refactor | 重构 |
| test | 测试相关 |
| chore | 构建/工具相关 |

### 示例

```
feat(core): 添加 WebSocket 支持

实现 RFC 6455 WebSocket 协议，支持：
- 文本帧和二进制帧
- Ping/Pong 心跳
- Close 帧
```

## 安全规范

### 敏感信息

- 禁止提交密钥、密码到代码库
- 配置文件中的敏感信息使用占位符
- `.gitignore` 已配置忽略敏感文件

### 依赖安全

- 定期检查依赖漏洞
- 使用 `mvn dependency:analyze` 检查未使用依赖

## 发布流程

1. 更新版本号
2. 运行完整测试：`mvn clean test`
3. 更新 CHANGELOG
4. 创建 Git Tag
5. 执行发布：`mvn clean deploy -P release`
