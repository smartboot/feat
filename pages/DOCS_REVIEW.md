# 文档站点评审记录

> 最近核对：2026-11-28  
> 范围：文档站点工程配置、文档缺口台账及 Router 文档重构后的状态。本文记录已核实的问题，不把尚未执行的构建或尚未逐篇检查的内容标记为通过。

## 当前结论

Router 文档已拆分为使用、匹配、拦截器和 Session 四个主题，并于 PR #9 合并。相关内容已对照当前 Feat Core 源码核对；该轮没有修改 Router 实现代码。

文档工程目前仍有一个优先级较高的阻塞项：`pages/package.json` 与 `pages/package-lock.json` 的根依赖声明不一致。当前环境没有可用的本地依赖安装与构建验证，因此不能将文档站点标记为构建通过。

## 已确认问题

### H3：文档站点 README 仍是初始化模板

当前 `pages/README.md` 仍保留 Astro Starlight Starter Kit 的默认介绍、StackBlitz/CodeSandbox/Netlify/Vercel 模板链接，以及“删除此文件”的提示。它没有说明 Feat 文档目录、开发命令、构建方式、文档维护约定或 Pages 部署流程，容易误导后续维护者。

已在本轮将其替换为 Feat 文档站点维护说明，补充目录结构、本地开发与构建命令、文档缺口台账入口、AI 文档索引检查要求和部署工作流位置。


### H1：package.json 与 package-lock.json 不一致

对照两个文件的根依赖声明，发现：

| 依赖 | package.json | package-lock.json |
|---|---|---|
| `astro-mermaid` | `^2.0.1` | `^1.3.1` |
| `starlight-llms-txt` | `^0.9.0` | `^0.7.0` |
| 贡献者插件 | `astro-contributors: ^0.8.0` | `starlight-contributor-list: ^0.3.0` |

这不是单纯的版本提示差异：根依赖名称也发生了变化。干净环境中的 `npm ci` 需要 package manifest 与 lockfile 保持一致，因此应先在具备 npm registry 访问能力的环境中重新生成锁文件，再执行安装和构建。

**处理建议**

1. 以当前 `pages/package.json` 为准，确认贡献者插件的实际使用与配置。
2. 在 Node/npm 版本符合项目要求的环境中运行 `npm install --package-lock-only` 或按项目约定重新生成 lockfile。
3. 执行 `npm ci` 与 `npm run build`。
4. 检查 Starlight 页面生成、站内链接及 `llms.txt` 输出。

在锁文件重新生成并通过 CI 前，不应手工猜测依赖树或声称构建已通过。

### H2：缺口台账中的 AOT 条目需要销账

`pages/DOCS_GAPS.md` 第 10 条仍将 `appendix/aot.mdx` 标记为待回写，并描述其大段内容仍在讲已删除的 `feat-cloud-aot-vm`。

本次读取当前 `pages/src/content/docs/appendix/aot.mdx`，页面已围绕 `feat-cloud-aot`、`feat-cloud-starter`、`CloudService`、`ServiceLoader` 和编译期生成流程展开，没有发现 `feat-cloud-aot-vm` 的旧模块叙述。因此台账状态与当前文件内容不一致，应更新为已回写，并在提交中记录对应依据。

## 已完成事项

- PR #9：Router 指南拆章，已合并。
- Router 路由匹配、拦截器链、Session 和异步处理器文档已按当前源码核对。
- `pages/DOCS_GAPS.md` 已登记本轮 Router 文档发现的缺口。

## 尚未验证

- 本轮未执行 `npm ci`。
- 本轮未执行 `npm run build`。
- 文档目录当前包含 50 篇 MDX 页面；尚未完成逐篇、逐链接审查。
- 尚未验证构建产物中的 `llms.txt` 是否完整包含新增 Router 章节。

## 后续顺序

1. 在有 npm registry 访问能力的环境中修复并验证 lockfile。
2. 完成全量文档逐篇评审，重点检查 API 与当前源码是否一致、跨页链接是否有效、版本边界是否明确。
3. 根据评审结果逐项更新 `DOCS_GAPS.md`，避免台账保留已经修复的旧问题。
