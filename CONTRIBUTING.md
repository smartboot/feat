# Feat 项目贡献指南

感谢您对 Feat 项目的关注！我们非常欢迎并感谢来自社区的贡献。本文档将指导您如何参与 Feat 项目的开发和改进。

## 目录

- [行为准则](#行为准则)
- [提交问题](#提交问题)
- [贡献代码](#贡献代码)
- [开发流程](#开发流程)
- [代码规范](#代码规范)
- [提交信息规范](#提交信息规范)
- [文档贡献](#文档贡献)
- [测试指南](#测试指南)
- [版本发布流程](#版本发布流程)
- [社区交流](#社区交流)

## 行为准则

为了营造一个开放、包容的社区环境，我们希望所有参与者都能遵守以下行为准则：

- 尊重每一位贡献者，无论其经验水平、性别、性取向、残疾状况、外貌、种族或宗教信仰如何
- 使用友好和包容的语言
- 尊重不同的观点和经验
- 优雅地接受建设性的批评
- 关注社区的最佳利益

## 提交问题

### 提交前检查

在提交新问题之前，请先检查以下事项：

1. 搜索现有的 [GitHub Issues](https://github.com/smartboot/feat/issues) 或 [Gitee Issues](https://gitee.com/smartboot/feat/issues)，确保没有重复的问题
2. 确认问题是否在最新版本中仍然存在
3. 检查文档和常见问题解答，确保问题未在其中解答

### 问题报告指南

提交问题时，请包含以下信息：

- **问题描述**：清晰简洁地描述问题
- **复现步骤**：详细的步骤，使我们能够复现问题
- **预期行为**：描述您期望看到的行为
- **实际行为**：描述实际发生的行为
- **环境信息**：
  - Feat 版本
  - Java 版本
  - 操作系统
  - 相关配置
- **日志或截图**：如有可能，提供相关日志或截图
- **可能的解决方案**：如果您有解决问题的建议，请一并提供

## 贡献代码

### 准备工作

1. Fork [Feat 仓库](https://github.com/smartboot/feat) 到您的 GitHub 账户
2. 将您的 fork 克隆到本地：
   ```bash
   git clone https://github.com/YOUR_USERNAME/feat.git
   cd feat
   ```
3. 添加上游仓库：
   ```bash
   git remote add upstream https://github.com/smartboot/feat.git
   ```
4. 创建新的分支：
   ```bash
   git checkout -b feature/your-feature-name
   ```

### 开发流程

1. 确保您的分支与上游最新代码同步：
   ```bash
   git fetch upstream
   git rebase upstream/master
   ```
2. 进行您的修改
3. 编写或更新相关测试
4. 确保所有测试通过
5. 提交您的更改：
   ```bash
   git add .
   git commit -m "feat: 添加新功能 XXX"
   ```
6. 将您的更改推送到您的 fork：
   ```bash
   git push origin feature/your-feature-name
   ```
7. 创建 Pull Request

### Pull Request 指南

创建 Pull Request 时，请：

- 提供清晰的标题和描述
- 引用相关的 issue（如果有）
- 描述您的更改解决了什么问题
- 列出您进行的主要更改
- 确保 CI 测试通过

## 代码规范

为了保持代码库的一致性和可维护性，请遵循以下规范：

### Java 代码规范

- 遵循 [Google Java 风格指南](https://google.github.io/styleguide/javaguide.html)
- 使用 4 个空格进行缩进，不使用制表符
- 类名使用 UpperCamelCase 风格
- 方法名、变量名使用 lowerCamelCase 风格
- 常量名使用 CONSTANT_CASE 风格（全大写，下划线分隔）
- 包名使用全小写
- 添加适当的 Javadoc 注释

### 代码质量要求

- 保持方法简短，单一职责
- 避免重复代码
- 编写单元测试覆盖您的代码
- 处理异常并提供有意义的错误信息
- 避免过度设计和不必要的复杂性

## 提交信息规范

我们使用 [Conventional Commits](https://www.conventionalcommits.org/) 规范来格式化提交信息：

```
<类型>[可选的作用域]: <描述>

[可选的正文]

[可选的脚注]
```

### 类型

- **feat**: 新功能
- **fix**: 修复 bug
- **docs**: 文档更改
- **style**: 不影响代码含义的更改（空格、格式化、缺少分号等）
- **refactor**: 既不修复 bug 也不添加功能的代码更改
- **perf**: 提高性能的代码更改
- **test**: 添加或修正测试
- **build**: 影响构建系统或外部依赖的更改
- **ci**: 对 CI 配置文件和脚本的更改
- **chore**: 其他不修改 src 或测试文件的更改

### 示例

```
feat(http): 添加 HTTP/2 服务器推送支持

实现了 HTTP/2 服务器推送功能，允许服务器在客户端请求之前发送资源。

Closes #123
```

## 文档贡献

文档对于项目的成功至关重要。您可以通过以下方式贡献文档：

- 修复文档中的错误或不准确之处
- 改进现有文档的清晰度和完整性
- 添加缺失的文档或示例
- 翻译文档

文档源文件位于项目的 `pages` 目录中。

## 测试指南

### 单元测试

- 为所有新功能和修复编写单元测试
- 使用 JUnit 5 进行测试
- 测试应该是独立的，不依赖于外部资源
- 使用模拟对象隔离被测代码

### 运行测试

```bash
mvn test
```

### 性能测试

对于性能敏感的更改，请运行性能测试：

```bash
mvn verify -P performance-tests
```

## 版本发布流程

Feat 项目遵循 [语义化版本控制](https://semver.org/) 规范：

- **主版本号**：当进行不兼容的 API 更改时
- **次版本号**：当以向后兼容的方式添加功能时
- **修订号**：当进行向后兼容的 bug 修复时

## 社区交流

- **GitHub Issues**: [https://github.com/smartboot/feat/issues](https://github.com/smartboot/feat/issues)
- **Gitee Issues**: [https://gitee.com/smartboot/feat/issues](https://gitee.com/smartboot/feat/issues)
- **邮件**: [zhengjunweimail@163.com](mailto:zhengjunweimail@163.com)

---

再次感谢您对 Feat 项目的贡献！您的参与对我们非常重要，我们期待与您一起打造更好的 Feat 框架。