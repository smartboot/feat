# 文档站点评审记录

> 最近核对：2026-11-28  
> 范围：文档站点工程配置、Router 文档重构后的状态，以及 Feat Cloud Bean、Controller、响应序列化、MyBatis、启动配置与许可口径的源码核对。本文记录已核实的问题，不把尚未执行的构建或尚未逐篇检查的内容标记为通过。

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

### H1：Feat Cloud 赞助提示与开源许可口径冲突

Feat Cloud 多个入口曾提示“需先成为赞助者获取授权”，包括快速开始页、全站 Cloud 章节提示组件、首页许可说明和仓库根目录 `llms.txt`。这与仓库 `LICENSE` 及 `sponsors.mdx` 的口径冲突：所有模块均采用 Apache 2.0，赞助是自愿支持，不构成使用条件。

对照 `feat-cloud-aot/license/LicenseLoader`：未配置 `license` / `FEAT_LICENSE` 时，处理器正常返回，不会因缺少赞助授权而阻止生成；只有显式提供 license 时才进行签名和名单校验。因此不能把可选的 license 校验描述成 Feat Cloud 的使用门槛。

已同步修正 Cloud 文档提示、首页许可说明、支付弹窗说明和 `llms.txt`。另在 Bean 章节补充 `@Autowired` 按字段名查找、setter 优先/反射回退，以及方法级 `@Bean` 当前实现的命名和生命周期边界。

### H1：CloudOptions 与 Profile 文档边界需要更精确

对照 `CloudOptions`、`AbstractCloudService`、`CloudOptionsSerializer` 和 `FeatAnnotationProcessor` 核对后，修正以下表述：

- `setPackages(...)` 是启动时对已生成 `CloudService` 的包名前缀过滤，不会改变编译期注解处理器生成哪些服务；底层判断使用 `startsWith`，不是严格包边界。
- 外部 `registerBean` 先注册，名称若与生成的类级或方法级 Bean 冲突，会在启动时触发重复名称异常。
- `server.autoSSL`、`server.session.store-type` 和 `server.session.timeout` 由特殊分支读取，不经过通用 `${}` 解析；Redis 的 `address` 也按字面值写入。
- `@Value("${app.name}")` 的路径从合并后的 YAML 读取；只有 YAML 对应值自身是 `${ENV_NAME}` 时，才会继续按系统属性 / 环境变量规则解析。
- 当存在多套 profile 时，指定不存在的环境名不会回退默认环境；系统属性优先于环境变量。

同时补充 `@Value` 支持的字段类型，并修正 `AGENTS.md` 中把运行时服务过滤描述成编译期扫描的注释，以及“完全没有反射装配”的绝对化表述。

### H1：Controller 请求绑定与 Gzip 文档需要补充实现边界

对照 `ControllerSerializer`、`RequestMapping`、`Param`、`PathParam`、`Controller` 和 `AsyncResponse` 核对后，补充以下边界：

- `@RequestMapping.method` 默认空数组，代表不限制 HTTP 方法；文档示例若只面向 GET 等特定方法，应显式声明。
- `@Param` 根据 Content-Type 从查询 / 表单参数或 JSON 对象取值，不只是 query string。
- `@PathParam` 当前支持 String、int / Integer、long / Long；数值转换失败时默认返回 0。
- `server.gzip` 与 `server.gzipThreshold` 可全局开启压缩并覆盖 Controller 阈值；相关特殊配置不走通用 `${}` 解析。
- 异步示例必须保证任务异常时也完成 `AsyncResponse`，并恢复线程中断标记。

### H1：MyBatis 集成文档与当前 AOT 实现需要对齐

对照 `MybatisSerializer`、`MapperSerializer` 和 `demo/mybatis` 核对后，补充并修正：

- Mapper 包装类每次调用使用 `openSession(true)`，自动提交并关闭；跨多条 SQL 的事务必须统一使用同一个手动提交的 `SqlSession` 和 `session.getMapper(...)`。
- 当前 AOT 数据源重建路径只支持 MyBatis `POOLED` DataSource；MyBatis XML 的配置会被解析后重新生成，不能假定所有自定义设置都会原样保留。
- `initial-sql` 在每次应用启动时执行；文档中的 `DROP TABLE` 仅适用于本地演示，不应作为生产环境的启动脚本。
- 修正事务示例中引用了未在前文声明的 Mapper 方法，并为 `@Autowired` 字段补上推荐的 setter。

另发现一项待单独处理的实现风险：`MybatisSerializer` 生成的初始化脚本代码直接使用 `sessionFactory.openSession().getConnection()`，没有保留并关闭 `SqlSession`。这可能造成连接长期占用；本轮只记录问题，不在未验证事务行为前修改运行时代码。

### H1：响应序列化文档与 JsonSerializer 实际路径不完全一致

对照 `JsonSerializer`、`ListSerializer`、`MapSerializer`、`DateSerializer` 和 `ControllerSerializer` 核对后，修正：

- Map 并不是编译期生成遍历代码，而是通过 FastJSON2 的 `JSONObject` 序列化；有泛型的 List 才生成遍历代码，原始 List 会回退。
- Controller 根返回值的直接写出路径主要是 String、byte[]、int 和 boolean；POJO 内的包装类型字段可生成直接写出逻辑，不能笼统说所有包装类型根返回值都直接写出。
- Date / Timestamp 作为 POJO 字段时默认输出毫秒时间戳，可通过 `@JSONField(format=...)` 自定义格式。
- 生成的 POJO 字段会保留 null；成功响应的 message 和失败响应的 data 因此应在示例 JSON 中显示为 null。

深层嵌套示例的分界按当前 `JsonSerializer.serialize(...)` 的递归层级核对，保留原有两组示例；异步响应仍在运行时走 FastJSON2。

## 已完成事项

- PR #9：Router 指南拆章，已合并。
- Router 路由匹配、拦截器链、Session 和异步处理器文档已按当前源码核对。
- Feat Cloud 的 Bean 注入、方法级 Bean、Controller 请求绑定、Gzip、响应序列化、MyBatis、CloudOptions 与 Profile 行为已对照 AOT 生成器和运行时上下文核对。
- Apache 2.0 与自愿赞助的许可口径已同步到 Cloud 文档、首页和 `llms.txt`。
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
