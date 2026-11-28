# 文档站点评审记录

> 最近核对：2026-11-28  
> 范围：文档站点工程配置、Router 文档重构后的状态，以及 Feat Cloud Bean、Controller、响应序列化、MyBatis、启动配置与许可口径的源码核对。本文记录已核实的问题，不把尚未执行的构建或尚未逐篇检查的内容标记为通过。

## 当前结论

Router 文档已拆分为使用、匹配、拦截器和 Session 四个主题，并于 PR #9 合并。相关内容已对照当前 Feat Core 源码核对；该轮没有修改 Router 实现代码。

文档工程目前仍有一个优先级较高的阻塞项：`pages/package.json` 与 `pages/package-lock.json` 的根依赖声明不一致。当前环境没有可用的本地依赖安装与构建验证，因此不能将文档站点标记为构建通过。

## 已确认问题

### H2：Nacos 生产鉴权示例缺少完整的 Feat 注入链路

对照 `pages/src/content/docs/cloud/nacos.mdx`、`demo/nacos/pom.xml`、`demo/nacos/src/main/resources/feat.yml` 与当前 Bean 配置注入约定核对后，发现原生产鉴权提示只展示了向 Nacos `Properties` 写入 namespace、username、password，却没有展示这些变量如何从 Feat 配置注入；若直接照抄片段，三个变量没有来源。

已在 Nacos 文档补充 `@Value` 字段、setter、环境变量/JVM 参数示例，以及将值写入 `Properties` 的完整片段。另明确当前最小配置监听示例只保存原始文本，生产业务应先解析和校验新配置，再替换当前配置快照。

当前 `demo/nacos` 使用 Nacos Client 3.2.3，示例配置覆盖 Config、Naming 注册和发现参数。Demo 的 `docker-compose.yaml` 仅用于本地单机环境，不应被理解为已经完整配置了生产鉴权。


### H2：XXL-Job 集成文档与 Demo 版本及配置示例不一致

对照 `pages/src/content/docs/cloud/xxljob.mdx`、`demo/xxljob/pom.xml`、`demo/xxljob/src/main/resources/feat.yaml` 和 Demo 中的 `XxlJobConfig` / `Bootstrap` 核对后，发现文档依赖版本写为 2.4.0，而 Demo 使用 3.4.0；文档中的 `accessToken` 占位符键与 Demo 实际配置的 `xxljob.accessToken` 不一致，环境变量名称也因此不准确。

同时，文档示例遗漏了配置 Bean 和 Bootstrap 的 setter，`enabled` 字段类型与 Demo 不一致，日志记录器还错误地引用了 `XxlJobConfig.class`。已将文档示例对齐当前 Demo，并补全 setter/getter 与系统属性、环境变量说明。此项只修改文档，不改 Demo 或依赖版本。


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

### H1：Feat Cloud 赞助授权口径被误改

Feat Core 与 Feat AI 采用 Apache 2.0；Feat Cloud 则采用赞助授权模式，企业通过赞助获取 Feat Cloud License。此前 PR #35 将 Cloud 与 Core、AI 一并标记为 Apache 2.0，并把赞助描述为可选支持，改变了既有授权规则。这是文档修改错误，不能据此推导 Cloud 的商业授权政策已经改变。

本次恢复统一口径：首页、许可说明、Cloud 快速开始、全站 Cloud 提示、赞助页、支付弹窗和仓库根目录 `llms.txt` 均明确区分 Core / AI 的 Apache 2.0 授权与 Cloud 的赞助 License 模式。赞助后由项目方提供 Feat Cloud License，具体适用范围以实际发放的授权信息为准。

另需区分商业授权规则与当前代码校验实现：当前 `LicenseLoader` 在未配置 License 时不会主动阻止注解处理器生成代码。本次修复的是官网及机器可读文档中的授权说明，没有擅自改动编译器行为；是否需要进一步强化无 License 时的构建期校验，应作为独立的代码任务评估。

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

另发现一项实现问题：`MybatisSerializer` 生成的初始化脚本代码直接使用 `sessionFactory.openSession().getConnection()`，且没有关闭 `Reader`。本次修复改为由 `try-with-resources` 同时管理 `SqlSession` 与 `Reader`，并通过 `SqlSession` 提供的连接执行脚本。MyBatis 3.5.11 的 `ScriptRunner.runScript` 会在执行结束时处理提交 / 回滚，但不会替调用方关闭连接；因此连接仍应由 `SqlSession.close()` 统一释放。

### H1：响应序列化文档与 JsonSerializer 实际路径不完全一致

对照 `JsonSerializer`、`ListSerializer`、`MapSerializer`、`DateSerializer` 和 `ControllerSerializer` 核对后，修正：

- Map 并不是编译期生成遍历代码，而是通过 FastJSON2 的 `JSONObject` 序列化；有泛型的 List 才生成遍历代码，原始 List 会回退。
- Controller 根返回值的直接写出路径主要是 String、byte[]、int 和 boolean；POJO 内的包装类型字段可生成直接写出逻辑，不能笼统说所有包装类型根返回值都直接写出。
- Date / Timestamp 作为 POJO 字段时默认输出毫秒时间戳，可通过 `@JSONField(format=...)` 自定义格式。
- 生成的 POJO 字段会保留 null；成功响应的 message 和失败响应的 data 因此应在示例 JSON 中显示为 null。

深层嵌套示例的分界按当前 `JsonSerializer.serialize(...)` 的递归层级核对，保留原有两组示例；异步响应仍在运行时走 FastJSON2。

### H1：Redisun 客户端未纳入 AOT 销毁流程

对照 `Redisun.close()`、`RedisunSerializer`、`CloudOptionsSerializer` 和 `FeatAnnotationProcessor` 核对后，发现：

- `feat.redis` 配置会在生成的 `loadBean()` 中创建 Redisun 客户端；客户端内部持有多路复用连接池、异步通道组和 BufferPagePool。
- Redisun 提供 `close()` 释放这些资源，但扩展序列化器此前只参与 import 和 Bean 初始化，没有把扩展字段及销毁逻辑写入生成类。
- 因此应用正常关闭时，Redisun 资源没有进入 Feat Cloud 的销毁流程。

本次修复让 `CloudOptionsSerializer` 调用扩展序列化器的 `serializeProperty()` 与 `serializeDestroy()`，由 `RedisunSerializer` 保存客户端实例，并在 `finally` 中调用 `redisun.close()`，确保某个 CloudService 的销毁逻辑抛出异常时，Redisun 仍会释放资源。 同时，`FeatCloud.cloudServer()` 在 `ApplicationContext.start()` 失败时也会调用 `application.destroy()`，清理启动过程中已经创建的资源。

## 已完成事项

- PR #9：Router 指南拆章，已合并。
- Router 路由匹配、拦截器链、Session 和异步处理器文档已按当前源码核对。
- Feat Cloud 的 Bean 注入、方法级 Bean、Controller 请求绑定、Gzip、响应序列化、MyBatis、CloudOptions 与 Profile 行为已对照 AOT 生成器和运行时上下文核对。
- Feat Core / AI 的 Apache 2.0 与 Feat Cloud 的赞助 License 授权口径已在本修复中重新统一。
- `pages/DOCS_GAPS.md` 已登记本轮 Router 文档发现的缺口。

## 尚未验证

- 本轮未执行 `npm ci`。
- 本轮未执行 `npm run build`。
- 当前文档目录包含 52 篇 MDX 页面；本轮已完成页面清单与结构元数据盘点，尚未完成逐篇内容、源码/API 与站内链接审查。
- 尚未验证构建产物中的 `llms.txt` 是否完整包含新增 Router 章节。

## 后续顺序

1. 在有 npm registry 访问能力的环境中修复并验证 lockfile。
2. 完成全量文档逐篇评审，重点检查 API 与当前源码是否一致、跨页链接是否有效、版本边界是否明确。
3. 根据评审结果逐项更新 `DOCS_GAPS.md`，避免台账保留已经修复的旧问题。



## 2026-11-28：知识架构第一阶段

本轮开始按“认识 Feat → 开始使用 → 构建业务应用 → 理解运行机制 → 扩展能力 → 工程实践 → 技术参考”的阅读过程调整文档入口。

- 重写 `guides/index.mdx`，明确 Core、AI、Cloud 的依赖关系，并给出 Core、Cloud、AI 三条入口路线。
- 新增 `building/index.mdx` 与 `building/first-business-application.mdx`，用任务管理示例串起 Controller、Bean、Service 和 HTTP 请求；示例使用内存存储，明确标注不具备持久化与跨实例同步能力。
- 首页增加“构建第一个业务应用”入口，并移除过时的 v2.4.2 徽标。
- 本轮曾错误地将 Feat Cloud 描述为 Apache 2.0，并把赞助改成可选支持；该表述现已在本修复中撤销，恢复 Cloud 通过赞助获取 License 的规则。
- 更新 Starlight 侧边栏、LLM 文档集合和 `pages/README.md`，纳入新的 `building/` 章节。

**验证状态：**本轮没有可用的本地 npm 安装与构建环境，尚未执行 `npm ci`、`npm run build`，也没有把构建标记为通过。需要依赖仓库 CI 对 Astro 页面、导航、Mermaid 和 `llms.txt` 生成进行验证。


## 2026-11-28：知识架构第二阶段启动

本轮重新读取 master 上的文档树，确认当前共有 52 篇 MDX 页面。已盘点每篇页面的 frontmatter、章节标题、代码导入和主要站内链接，逐篇内容与源码/API 的核验仍在进行，不能把元数据盘点等同于完整审查。

初步发现：

- astro.config.mjs 的侧边栏仍使用“理解 Feat”，而入口页标题已改为“认识 Feat”，导航术语需要统一。
- cloud/mybatis.mdx 仍是一篇独立的 User CRUD 教程；新建的 building/first-business-application.mdx 已使用 Task CRUD。两篇承担相似的 Controller → Service → 数据访问链路，后续应将 MyBatis 文档改成前一篇任务应用的持久化扩展，避免读者重复学习第二套业务。
- Cloud Controller 同时支持 `/:name` 与 `/{name}` 两种路径参数写法，并会在编译期转换为 Core Router 使用的冒号形式；Core Router 本身只支持 `/:name`。MyBatis 示例中的花括号写法并非错误，但本轮改用冒号形式，以和新业务教程保持一致。
- `chatStream` 在 v2.5.0 中从 `void` 改为 `CompletableFuture<ChatResponse>`。为避免第一篇 Quick Start 被版本差异影响，基础示例改用 `chat(...).get()` 展示完整响应；流式调用及其版本边界留在对话模型章节说明。
- 旧的《Feat 官网文档设计》上传稿记录的是 2.1.1 时期架构，并把 Feat AI 描述为仅增强 HTTP Client 的层次；该描述与当前已经确认的 Core → AI → Cloud 依赖关系不一致，不能作为当前架构文案依据。

本阶段优先处理阅读路径与重复内容，再继续按主题核对示例、API、链接和 PDF 阅读体验。


### 第二阶段本轮处理记录

- 将 `cloud/mybatis.mdx` 从独立的 User CRUD 教程改为前一篇任务管理应用的持久化扩展：沿用 `Task`、`TaskController` 和现有 HTTP 接口，只新增 MyBatis 配置、Mapper，并替换 Service 的存储实现。
- 修正 MyBatis 示例中的 shell 路径占位符，避免将尖括号占位符放进可直接复制执行的命令。
- 将 AI Quick Start 的两个示例统一为 `chat(...).get()`，让命令行程序等待完整响应；把流式 API 的版本差异留在对话模型章节，避免 Quick Start 依赖 v2.5.0 才提供的 `chatStream` Future 返回值。
- 将侧边栏“理解 Feat”统一为“认识 Feat”，并修正 `DOCS_GAPS.md` 中错误的 Cloud 授权描述。
- 对 52 篇 MDX 页面中的 Markdown 形式站内 `/feat/...` 链接做了路径存在性检查，未发现失效目标。该检查不覆盖 JSX `href`、图片导入、锚点、代码示例编译和 Astro 构建。

**仍未验证：**本轮未执行 `npm ci`、`npm run build`，也未逐段编译全部代码示例。MyBatis 文档中的配置与 API 说明已结合当前文档记录和仓库 Demo 交叉核对，但仍需 CI 和真实示例项目验证。


## 2026-11-28：Core Router 请求生命周期复核

本轮重新读取 master 中的 `Router`、`NodePath`、`RouterHandlerImpl`、`Chain` 和 `RouterHandler`，并与上传的 Router 源码摘录对照。确认了一个需要写进文档的协议差异：

- HTTP/1.x 在 `Router.onHeaderComplete(HttpEndpoint)` 中按 URI 调用 `matchHandler`，并将选出的处理器设置到当前 `HttpEndpoint`；后续处理沿用该 Handler。
- HTTP/2 在 `Router.handle(HttpRequest, CompletableFuture)` 中按当前请求 URI 调用 `matchHandler`，再调用匹配到的 Handler。
- 当前 master 的 `Router.onHeaderComplete` 不包含向 `uriByteTree` 注册 URI 的逻辑。上传源码摘录中的该行不能直接当作当前 master 的实现依据。
- URI 路径匹配与 HTTP 方法选择是两个阶段：先由 `NodePath` 找到路径处理器，再由 `RouterHandlerImpl#getRouterUnit` 选择方法处理器；方法不匹配时回退到 Router 默认处理器。
- 已将协议请求阶段、处理器选择入口和源码阅读顺序补入 `server/router-matching.mdx`。未修改 Router 运行时代码。

本轮继续确认此前关于模式重叠、拦截器短路、异步推进限制和 Session 惰性创建的文档表述与当前源码一致。当前上传源码摘录与 master 在 `uriByteTree` 细节上存在版本差异，后续以仓库 master 为准。

**验证状态：**本轮只做源码与 Markdown 内容核对，未执行 `npm ci`、`npm run build` 或 Router 集成测试。Astro 构建及文档示例编译状态仍未验证。


## 2026-11-28：HTTPS 与 SSE 示例复核

本轮对照 Feat Core 的 `HttpRequest` / `HttpProtocol`，以及 Feat 当前依赖的 smart-socket 2.2.0 对应 API，检查 HTTPS 与协议升级示例，发现并修正：

- `HttpRequest#getProtocol()` 返回 `HttpProtocol` 对象，不是 `String`。读取可展示的协议名称应调用 `getProtocol().getProtocol()`；修正 HTTPS 示例中的类型错误和 HTTP/2 示例输出。
- smart-socket 的 `ClientAuth` 选项为 `NONE`、`OPTIONAL`、`REQUIRE`，不存在 `REQUEST`；更新说明。
- `PemServerSSLContextFactory` 实现的是 `SSLContextFactory`，不是 `ServerSSLContextFactory`；当前 `SslPlugin` 没有文档原先展示的按域名 Lambda 构造方式。删除不可用的多证书示例，明确 SNI 多证书需要自定义 JSSE `SSLContext` / `KeyManager`。
- mTLS 示例原先既未设置 `SSL_ENGINE_THREAD_LOCAL`，也将 `ClientAuth.REQUIRE` 与 PEM factory 组合成不匹配的构造调用。改为使用 `SslPlugin(SSLContextFactory, Consumer<SSLEngine>)`，在回调中启用 `setNeedClientAuth(true)` 并关联当前请求的 `SSLEngine`；证书读取改用标准 `java.security.cert` API。
- SSE 定时推送示例原先调用 `complete()` 后没有停止 `scheduleAtFixedRate`，会在连接关闭后继续运行。示例现在在正常结束和发送异常时分别关闭调度器，并补充生产环境应管理调度器生命周期的说明。

本轮只修改文档示例和评审记录，没有修改 Feat 或 smart-socket 运行时代码。

**验证状态：**已对照 Feat master 与 smart-socket 2.2.0 API 源码；未执行 Maven 示例编译、`npm ci` 或 `npm run build`。示例的实际编译与 Astro 构建仍待 CI / 本地环境验证。


## 2026-11-28：Core 客户端 API 与生命周期复核

本轮对照 Feat Core 的 `HttpClient`、`HttpRest`、`HttpRestImpl`、`HttpOptions`、`SseClient`、`SseEvent`、`WebSocketClient`、`WebSocketOptions`、`WebSocketListener` 与 `Feat` 工厂方法，复核客户端章节，发现并修正：

- HTTP 客户端采用 `MultiplexClient` 复用连接，不能在客户端总览中概括为每次请求都建立短连接。
- `HttpRest#onClose` 当前通过 `CompletableFuture.thenRun` 注册，只在 Future 正常完成时触发；异常完成走 `onFailure`，不会触发 `onClose`。HTTP 4xx/5xx 是正常收到的 HTTP 响应，应用应检查状态码。
- `onSSE()` 与 `onResponseHeader()` 使用同一个响应头回调槽位，后注册的会覆盖先注册的；识别为 SSE 后会切换事件解析，`onResponseBody()` 不再接收原始数据块。
- `SseEvent` 是类而不是接口，除 ID / type / data 外还提供 retry 与 comment 读取方法；事件 ID 不会自动实现游标保存或断线续传，`Last-Event-ID` 需要应用自行维护。
- SSE 文档中的自动关闭示例原先引用未完成赋值的局部变量，改用 `AtomicReference`；定时关闭与服务端定时推送示例均补上调度器清理，避免遗留线程。
- `WebSocketOptions` 当前没有 `readTimeout`、`writeBufferSize`、`maxFrameSize`；`WebSocketClient` 没有 `header()`、`isConnected()` 或 `close(code, reason)`。请求头应通过 `options().addHeader(...)` 配置，关闭使用无参 `close()`。
- `WebSocketListener#onError` 的签名包含 `WebSocketResponse` 参数；握手非 101 的处理和底层连接异常不是同一回调路径，不能承诺 `onClose` / `onError` 一定覆盖所有网络故障。
- 修正 WebSocket 示例中的 `getStatusCode()` 为 `statusCode()`，并补齐示例导入与应用侧连接状态管理。

本轮只修改客户端文档与评审台账，没有修改运行时代码。

**验证状态：**已对照当前 master API 源码；未执行 Maven 示例编译、`npm ci` 或 `npm run build`。客户端代码示例与 Astro 构建仍待实际编译验证。


## 2026-11-28：Feat AI 学习路径与记忆机制复核

本轮对照 Feat AI 的 `ChatModel`、`Provider`、`ChatResponse`、`ReActAgent`、`AgentOptions`、`MemoryOptions`、`InMemoryMemory`、`VectorMemory`、`EmbeddingModel` 与相关文档，复核 AI 学习路径并修正：

- Quick Start 原先声明支持本地 Ollama / 云端模型两种方式，但 Tabs 只有 Ollama 一个选项；补齐 Gitee AI 云端模型示例，并说明本地 Ollama 不需要云端 API Key。
- 非流式 `ChatModel.chat(...)` 遇到非 200 HTTP 响应时会返回 `success=false` 的 `ChatResponse`，不是 Future 异常完成；流式调用对未建立有效 SSE 的 HTTP 错误会触发 `onError` 并使 Future 异常完成。已按两种路径分别说明。
- Agent 最小示例删除 `getState()` 输出：当前 `ReActAgent` 只更新 RUNNING / TOOL_EXECUTION，没有在完成或失败时写入 FINISHED / ERROR，状态值不能作为终态依据。
- Agent 文档补充 `maxIterations` 的用途，并链接到自定义 `AgentTool` 实现示例。
- Memory 示例将 Agent 检索数量配置改为 `AgentOptions.memoryTopK`；说明 `MemoryOptions.defaultTopK` 只作用于直接调用 `Memory.search(query)`。
- `Memory.clear()` 清除全部会话；当前会话应调用 `clear(sessionId)`。已修正直接 Memory API 示例。
- 当前 `InMemoryMemory` 虽按 sessionId 分桶存储，但 `search()` / `getRecent()` 会遍历所有会话桶，未应用 `sessionIsolation` 过滤。文档已明确警告，不应将其当作多用户隔离边界。
- `VectorMemory` 默认启用 sessionIsolation，但配置中的 sessionId 默认为 null；写入时会为缺失的消息 sessionId 填充默认值，检索则按配置 sessionId 过滤。文档示例现在显式设置 Agent sessionId。
- `VectorMemory.clear()` 当前只清理本地缓存，没有删除持久化 VectorStore 中的数据；`clear(sessionId)` 才会尝试删除指定会话的数据。此行为与 Memory 接口的“清除所有记忆”描述不一致，列为待代码修复项。
- `EmbeddingModel.embed()` 当前是同步阻塞调用，文档补充线程使用提醒。

**仍需单独处理的运行时代码问题：**

1. InMemoryMemory 的会话检索隔离没有真正生效，存在跨 session 记忆串读风险。
2. VectorMemory 的默认 sessionId 与写入时默认 session 标识不一致，未显式配置时可能写入后无法按预期检索。
3. VectorMemory.clear() 未清理持久化 VectorStore。
4. EmbeddingModel 请求体硬编码 `dimensions=1`，而 VectorMemoryOptions 默认 `vectorDimension=1024`；需要结合目标 Provider 实际返回维度验证并修正。
5. ReActAgent 未更新 FINISHED / ERROR 终态。

本轮只修改文档与评审台账，没有修改 Feat AI 运行时代码。

**验证状态：**已对照当前 master 源码；未执行 AI 示例 Maven 编译、`npm ci` 或 `npm run build`。向量记忆示例在上述运行时问题修复前不应被当作已验证的生产配置。

## 2026-11-28：Core 服务端文档第二轮复核

本轮重新读取 Core 服务端文档与 master 源码，重点检查 Router、异步处理、ServerOptions、HTTPS 和协议升级章节之间的衔接，并完成以下调整：

- `server/router-matching.mdx`：补充模式分支由 `HashMap` 保存、遍历顺序不构成 API 契约的说明；进一步明确终端路径参数与通配符叶子共用内部特殊键，不能将 `/users/:id` 和 `/users/*` 当作两个可独立竞争的候选路由注册。补充多个后缀模式重叠时也不应依赖隐含顺序。
- `server/async.mdx`：将“主线程”改为更准确的当前请求处理线程；删除“超过 10ms 就应异步”的固定阈值，改为按阻塞性质、线程隔离和异步编排成本判断。异步示例补充线程池拒绝任务时的 503 响应与 Future 完成处理。
- `server/serveroptions.mdx`：修正 `headerLimiter` 超限行为。当前解析器达到上限后会忽略后续 Header 字段并继续解析请求，不会仅因数量超限返回 400；同步修正参数表述。
- `server/index.mdx`：在 Router 与异步章节之后增加 HTTPS 和协议升级的阅读入口，形成从请求处理到传输能力的连续路径。
- `server/https.mdx` 与 `server/upgrade.mdx`：调整侧边栏顺序，避免与 Router Session、拦截器章节出现重复排序值。
- 清理 async、ServerOptions、HTTPS 页面未使用的 Starlight 组件导入。

源码核对范围包括 `Router`、`NodePath`、`RouterHandlerImpl`、`Chain`、`RouterHandler`、`Context`、`HttpHandler`、`HttpMessageProcessor`、`HttpRequestProtocol`、`ServerOptions`、`HttpServer`、`SessionManager` 与 `LocalSessionManager`。本轮未修改运行时代码。

**验证状态：**已重新读取本轮修改后的 9 篇服务端页面；Markdown 代码围栏数量均为偶数，检查到的 Markdown 站内链接均指向已存在的章节。未执行 `npm ci`、`npm run build` 或 Maven 示例编译；因此 Astro 构建、示例编译和运行行为仍未验证。
## 2026-11-28：工程实践文档复核

本轮对照 Core 服务端源码复核安全、静态资源、日志、部署、测试、性能调优、Spring 迁移与 FAQ 页面，修正了以下内容：

- `practices/security.mdx`：修复 BasicAuth 示例中 Core `Context` 不存在的 `ctx.write()` 调用；将 Session 主入口改为 Core Router Session，并明确 Core 默认本地 Session 与 Cloud Redis Session 的边界；补充静态资源路径检查不覆盖符号链接越界的限制。
- `practices/static-files.mdx`：修正文件目录与 classpath 模式的无扩展名资源回退差异；明确 `autoIndex` 当前只在文件目录分支提供目录列表；说明路径穿越检查是 URL 解码后的字符串检查，不等同于规范化目标路径和符号链接校验。
- `practices/logging.mdx`：修复 `HttpHandler` 示例错误使用 `ctx.write()` 的问题；根据 `RunLogger.log()` 的执行顺序，说明级别刷新不是后台定时轮询，而是在日志调用通过当前级别过滤后才检查系统属性。
- `practices/deploy-core.mdx`：修复 Core `HttpHandler` 示例；纠正 `shutdownHook` 的语义。它不会收到请求上下文，且当前 `HttpServer.shutdown()` 在执行回调后才关闭底层服务，因此不能把它描述为停止接收新请求并排空在途请求的完整优雅停机机制。
- `practices/testing.mdx`：修复测试服务器的 `HttpHandler` 示例，并补充 Cloud 集成测试片段中缺失的端口就绪等待辅助方法。
- `practices/from-spring.mdx`：修正 `@Param` 仅绑定查询参数的表述，说明其按 Content-Type 从查询参数、表单参数或 JSON 对象取值。
- `practices/faq.mdx`：澄清 Core Router 与 Cloud Controller 是不同层次的 API；移除“外部调用和数据库访问必须异步”的绝对化建议；同步静态资源安全边界。
- `practices/performance.mdx`：删除固定的 1ms / 10ms 异步判断阈值；纠正 ByteTree 与 URI 映射树的职责边界，明确当前 Core Router 使用 `NodePath`，不会自动注册到 `uriByteTree`；补充 classpath 静态资源当前无条件设置 gzip 响应头的实现细节。

源码核对范围包括 `BasicAuthServerHandler`、`WafOptions`、`WAF`、`HttpStaticResourceHandler`、`FileServerOptions`、`RunLogger`、`LoggerFactory`、`HttpServer`、`ServerOptions`、`HttpHandler` 和 `HttpRequestProtocol`。本轮只修改文档，没有改动运行时代码。

### 发现的实现层后续事项

1. `HttpStaticResourceHandler` 的 classpath 分支当前直接设置 `Content-Encoding: gzip`，没有根据请求的 `Accept-Encoding` 判断客户端是否接受 gzip。文档已注明这一行为；建议后续单独评估实现修复。
2. 静态资源文件目录模式只检查解码后路径中是否包含 `../` 或 `..\`，没有对最终目标执行 canonical-path containment 校验。资源目录中若存在指向目录外的符号链接，不能把当前检查当作完整的文件系统隔离边界。建议后续评估安全加固。

**验证状态：**已重新读取本轮修改的 8 篇实践页面并检查 Markdown 代码围栏闭合；对示例中已知的 `ctx.write()` 错误进行了修复。未执行 `npm ci`、`npm run build` 或 Maven 示例编译，不能将 Astro 构建或示例编译标记为通过。