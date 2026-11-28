# Feat 文档站点

本目录是 Feat 官方文档站点，基于 [Astro](https://astro.build/) 与 [Starlight](https://starlight.astro.build/) 构建。

正式文档内容位于 `src/content/docs/`，按 Feat 的模块与阅读场景组织。修改教程时，应以仓库当前源码和可运行示例为依据，不要直接沿用 Spring 或其他框架的惯例。

## 本地开发

需要 Node.js 24（与 GitHub Pages 工作流保持一致）及 npm。

在本目录执行：

```bash
npm install
npm run dev
```

开发服务器启动后，按终端输出的地址访问。

## 构建与预览

```bash
npm run build
npm run preview
```

生产构建输出到 `dist/`。提交文档变更前，尽量完成一次生产构建，并检查新增页面、站内链接和控制台错误。

当前 `package.json` 与 `package-lock.json` 的依赖声明存在漂移。在锁文件重新生成并验证之前，不要把 `npm ci` 或构建视为已通过；具体问题和处理状态见 [DOCS_REVIEW.md](./DOCS_REVIEW.md)。

## 目录结构

```text
pages/
├── public/                  # 不经内容处理的静态资源
├── src/
│   ├── assets/              # 由 Astro 处理的图片等资源
│   ├── content/
│   │   └── docs/            # 正式文档（MDX）
│   └── styles/              # 站点样式
├── astro.config.mjs         # Starlight 导航、插件与站点配置
├── package.json
└── package-lock.json
```

文档页面按目录自动生成侧边栏：

- `guides/`：认识 Feat 与设计原理
- `building/`：从第一个接口到完整业务应用
- `practices/`：安全、部署、性能、测试等工程实践
- `cloud/`：Feat Cloud
- `ai/`：Feat AI
- `server/`：Feat Core 服务端
- `client/`：Feat Core 客户端
- `appendix/`：附录

新增页面时，按同目录页面的写法添加 frontmatter，至少包含 `title`、`description`，并设置合适的 `sidebar.order`。

## 文档维护约定

- 先读当前实现和已有文档，再动笔；源码与文档冲突时，以当前源码为准。
- 示例必须对应真实 API，不能只保证看起来像 Java 或 Spring。
- 解释行为时，区分 Feat Core 与 Feat Cloud，不要把两套编程模型混为一谈。
- 新发现的文档缺口登记到仓库根目录的 `pages/DOCS_GAPS.md`；全量审查结论记录在 `pages/DOCS_REVIEW.md`。
- 页面内容尽量保持 Markdown/PDF 友好，避免为了网页交互而引入不必要的复杂组件。
- `starlight-llms-txt` 会从文档内容生成面向 AI 工具的索引文件；新增、移动或重命名页面后，应检查构建产物中的 `llms.txt` 与 `llms-full.txt`。

## 部署

推送涉及 `pages/**` 的提交到 `master` 后，GitHub Actions 中的 Pages 工作流会构建并部署文档站点。工作流定义位于仓库根目录的 `.github/workflows/pages.yml`。
