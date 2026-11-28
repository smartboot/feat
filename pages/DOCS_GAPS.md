# 文档缺口台账

用 Feat 写业务时撞到的文档问题，逐条登记、回写后销账。

与 [`DOCS_REVIEW.md`](./DOCS_REVIEW.md) 的分工：那份记录**通读评审进度**（目标范围为当前 52 篇 MDX 页面，逐步找出过期、矛盾、断链）；这份是**开发过程中实际撞到的缺口**——按代码写不动、写错了才记下来，因为"实际卡住过"比"看起来可以更好"更能说明文档该补什么。

## 使用方式

- **动工前**：先查这张表。如果手上要做的事已在表里，说明文档确实没写清，直接看「结论」列的源码依据，不要重新摸索一遍。
- **收工前**：本次新撞到的缺口追加到表里。补完文档把状态改成 `已回写` 并填 commit。
- 状态：`待回写` / `已回写` / `不补（有意为之）`

## 台账

| # | 主题 | 现象 | 源码依据 | 应补在哪 | 状态 |
|---|---|---|---|---|---|
| 1 | 通用缓存 | 全站 `grep -E "缓存\|cache"` 只命中 ByteTree 字节树、静态资源 `Last-Modified`、拦截器"可用于缓存命中"，**没有业务缓存章节**；读者会以为要自己引 Caffeine/Redis | `feat-cloud/pom.xml` 已依赖 `tech.smartboot:redisun`（optional），`RedisunSerializer` 编译期注册 bean `redisun` | `cloud/cache.mdx`（新增） | 已回写 |
| 2 | Redis 配置键 | `session.mdx`、`troubleshooting.mdx` 写 `feat.redis.host` + `port`，**这两个键无人读取**，配了等于没配 | `RedisunSerializer.serializeLoadBean` 只读 `$.feat.redis.address` / `username` / `password` / `database` | `cloud/session.mdx`、`cloud/troubleshooting.mdx` | 已回写 |
| 3 | Redis Session 前提 | 文档没说 `store-type: redis` 要求同时存在 `feat.redis` 块 | `CloudOptionsSerializer` L122 判断 store-type，L330 生成 `getBean("redisun")`；而 `redisun` bean 只在 `$.feat.redis != null` 时由 `RedisunSerializer` 注册 → 只配一半能编译通过、启动取不到 bean | `cloud/session.mdx` | 已回写 |
| 4 | 计算属性不输出 | `CacheStats.getHitRate()` 没有对应字段，接口返回里静默少了 `hitRate` | 生成的 `XxxControllerCloudService` 把返回值拼成 JSON 字节，逐字段调 getter；`@PostConstruct` 的返回值不经过运行时序列化 | `cloud/cache.mdx`、`AGENTS.md` | 已回写 |
| 5 | 启动异常被解包 | 启动期 `new IllegalStateException(msg, e)` 的 msg 在控制台看不到，只剩 `Connection refused` | `FeatException(String, Throwable)` 调用 `getRootCause(cause)`，一路解包到最底层 | `cloud/cache.mdx`、`AGENTS.md` | 已回写 |
| 6 | 改 `feat.yml` 后 `mvn compile` 不重新生成 | 只改 yaml 后跑 `mvn compile`，装配代码仍是旧的 `setProvider("memory")`，`clean` 后才变成新值 | Maven 增量构建未触发注解处理器 | `AGENTS.md` 第 12 条、`cloud/cache.mdx` | 已回写 |
| 7 | MyBatis 会话与事务 | `mybatis.mdx` 全文无 `sessionFactory` / `SqlSession` / 事务；末尾只写"后面继续扩展权限、事务、校验时结构不会乱" | `MybatisSerializer` 在 `loadBean` 末尾 `addBean("sessionFactory", ...)`，`@Autowired` 在之后的 `autowired` 阶段，所以可直接注入 | `cloud/mybatis.mdx` 增「会话与事务」一节 | 已回写 |
| 8 | MyBatis 示例教反了 | `demo/mybatis` 的 Service 用 `catch (Exception e) { e.printStackTrace(); return false; }`，正是会让事务失效的写法 | `demo/mybatis/src/main/java/.../service/` | 改示例 + 在 `mybatis.mdx` 点明为什么不 catch | 已回写（示例已去掉 try/catch） |
| 9 | `@Value` 写法 | `bean.mdx` 写 `@Value("app.name")`，`nacos.mdx` / `xxljob.mdx` 写 `@Value("${...}")`，`options.mdx` 说必须带 `${}` | 以 AOT 生成代码为准：不带 `${}` 注入的是字面量 | `cloud/bean.mdx` | 待复核（`bean.mdx` L122 已明确说明，H2 可能已修） |
| 10 | `appendix/aot.mdx` | 旧记录称整页约一半在讲已删除的 `feat-cloud-aot-vm` 模块；复核当前页面后未发现该旧模块叙述 | 当前 `appendix/aot.mdx` 已围绕 `feat-cloud-aot`、`feat-cloud-starter`、`CloudService` 与 `ServiceLoader` 展开 | `appendix/aot.mdx` | 已回写（2026-11-28 复核；见 DOCS_REVIEW H2） |
| 11 | 嵌套层数决定序列化路径 | `response.mdx` 的表格只写「数组、`Object`、嵌套过深对象 → 回退到 FastJSON2」，没说「深一层」的判据，更没说回退后**字段集合会变**。实测 `RestResult → LoginResponse → CurrentUserResponse → List<MenuItem>` 里的元素省略了值为 `null` 的字段，而同样这些元素在 `RestResult → CurrentUserResponse → List<MenuItem>` 里是逐字段写出、包含 `"permission": null` | 生成的 `AuthControllerCloudService`：`/api/auth/login` 用 `os.write(JSON.toJSONBytes(p3))`，`/api/auth/me` 展开成 `p2.getXxx()` | `cloud/response.mdx` 加 caution | 已回写 |
| 12 | Router 异步处理器 Lambda | 旧示例使用 `(context, future) ->` 注册 Router 处理器，但 `RouterHandler` 只有一个抽象方法，双参数重载是默认方法，示例无法编译 | `RouterHandler#handle(Context)` 是唯一抽象方法；异步实现需匿名类重写 `handle(Context, CompletableFuture)` | `server/router.mdx`、`server/async.mdx` | 已回写（`d5dd22e` / `f07bcd8`） |
| 13 | SessionManager 类型 | 旧文档称其为接口，指导用户 implements | `SessionManager` 是抽象类，存储扩展点是 `getSession(String)` 与 `createSession()` | `server/router-session.mdx` | 已回写（`9781278`） |
| 14 | 拦截器短路与异步推进 | 旧示例手动完成 Future，未说明 Router 在链路中断后会完成 Future；也未说明延迟异步调用 `chain.proceed` 的限制 | `Router#matchHandler` 在 `chain.proceed` 返回后检查 `Chain#isInterrupted` 并完成 Future | `server/router-interceptors.mdx` | 已回写（`27aabcd`） |
| 15 | 路由匹配优先级 | 旧文档承诺精确 > 参数 > 通配符的全局优先级，无法覆盖重叠模式的实际行为 | `NodePath` 使用 `HashMap` 保存模式节点，比较深度且通配叶子存在提前结束分支；终端参数与通配符使用同一特殊键 | `server/router-matching.mdx` | 已回写（`1309e15`） |
| 16 | Feat Cloud 赞助授权口径 | 曾将 Cloud 与 Core、AI 一并写成 Apache 2.0，并把赞助描述为可选支持；该口径已确认错误 | 项目既有商业约定：Core / AI 为 Apache 2.0；Cloud 通过赞助获取 Feat Cloud License。`LicenseLoader` 当前未配置 License 时仍会生成代码，这是实现校验问题，不改变授权约定 | 首页、Cloud 快速开始、赞助页、支付弹窗、`llms.txt` | 已回写（2026-11-28；见 DOCS_REVIEW H1） |
| 17 | `setPackages` 语义 | 文档把它称为包扫描范围，容易理解为编译期少生成代码 | `AbstractCloudService.acceptService` 在运行时用生成类名 `startsWith(pkg)` 过滤服务；注解处理器仍会生成服务代码 | `cloud/options.mdx`、`AGENTS.md` | 已回写（2026-11-28；见 DOCS_REVIEW H1） |
| 18 | 配置占位符特殊边界 | 文档泛称 `server.*` 都支持 `${}`，并容易把 `@Value` 的 YAML 路径与外部环境变量解析混为一谈 | `CloudOptionsSerializer` 对 `autoSSL`、Session 特殊键单独读取；`RedisunSerializer` 对 `address` 直接写入；`serializerValueSetter` 先按 YAML 路径取值，再由类型 Serializer 解析值 | `cloud/options.mdx`、`cloud/bean.mdx` | 已回写（2026-11-28；见 DOCS_REVIEW H1） |
| 19 | Controller 请求绑定边界 | 文档只把 `@Param` 描述为查询参数，未说明 JSON / 表单来源；未说明路径参数支持类型和默认转换值 | `ControllerSerializer` 根据 Content-Type 选择 JSON 或 request parameters；`@PathParam` 仅支持 String、int/Integer、long/Long，数值转换默认 0 | `cloud/controller.mdx` | 已回写（2026-11-28；见 DOCS_REVIEW H1） |
| 20 | Controller 方法限制与 Gzip | 文档未说明 `RequestMapping.method` 默认不限制 HTTP 方法，也未说明全局 Gzip 配置 | `RequestMapping.method()` 默认空数组；`ControllerSerializer` 读取 `server.gzip` / `server.gzipThreshold` 覆盖控制器配置 | `cloud/controller.mdx`、`cloud/options.mdx` | 已回写（2026-11-28；见 DOCS_REVIEW H1） |
| 21 | MyBatis 数据源与配置重建 | 文档未说明 AOT 只支持 POOLED DataSource，且 XML 中自定义配置不一定完整保留 | `MybatisSerializer` 仅对 `PooledDataSource` 生成重建代码，并显式复制有限的 Configuration 属性 | `cloud/mybatis.mdx` | 已回写（2026-11-28；见 DOCS_REVIEW H1） |
| 22 | 响应序列化路径 | 文档把 Map 和所有包装类型都描述为编译期直接写出，遗漏了 Map / 数组 / 原始 List 的 FastJSON2 回退路径 | `MapSerializer` 使用 `JSONObject`；`ListSerializer` 仅对声明泛型的 List 生成遍历；`JsonSerializer` 对数组、Object、深层类型回退 | `cloud/response.mdx` | 已回写（2026-11-28；见 DOCS_REVIEW H1） |
| 23 | RestResult JSON 示例 | 示例省略了生成序列化代码会输出的 null 字段 | `RestResult` 的四个字段均参与 POJO 序列化；生成代码对 null 写出 `null` | `cloud/response.mdx` | 已回写（2026-11-28；见 DOCS_REVIEW H1） |

| 24 | Router 匹配的协议时机 | 原文只描述 URI 到达 Router 后的统一匹配，未区分 HTTP/1.x 与 HTTP/2 的调用入口；上传的旧源码摘录还包含当前 master 已不存在的 `uriByteTree.addNode` | 当前 master 的 `Router.onHeaderComplete` 在 HTTP/1.x 请求头接收完成时匹配并设置 Handler；HTTP/2 在 `Router.handle` 中按 URI 匹配；当前 master 不含该 `uriByteTree` 注册语句 | `server/router-matching.mdx` | 已回写（2026-11-28；本轮复核） |
| 25 | HTTPS API 类型与 TLS 示例 | HTTPS 文档把 `HttpProtocol` 当成 String，ClientAuth 列举了不存在的 `REQUEST`，多证书 Lambda 构造与当前 `SslPlugin` API 不匹配；mTLS 示例缺少请求级 SSLEngine 关联；SSE 定时任务在连接关闭后仍继续调度 | Feat `HttpRequest#getProtocol()` 返回 `HttpProtocol`；smart-socket 2.2.0 的 `ClientAuth` 只有 `NONE/OPTIONAL/REQUIRE`，`SslPlugin` 构造器接收 `SSLContextFactory` 与可选 Consumer；`SseEmitter.complete()` 只关闭连接，不负责取消应用创建的定时任务 | `server/https.mdx`、`server/upgrade.mdx` | 已回写（2026-11-28；本轮复核） |
| 26 | Core 客户端 API 与回调边界 | 客户端文档将可复用的 HTTP 连接描述为短连接；`onClose` 被描述成成功/失败都会触发；SSE 示例把 `SseEvent` 写成接口、自动续传描述不清且存在调度器泄漏；WebSocket 文档调用不存在的配置方法、头设置方法、连接状态方法和 close 重载，并使用错误的 `onError` 签名 | `HttpClient` 使用 `MultiplexClient`；`HttpRestImpl#onClose` 使用 `thenRun`；`SseEvent` 是类；`WebSocketOptions` / `WebSocketClient` / `WebSocketListener` 当前公开 API 与文档调用不一致 | `client/index.mdx`、`client/http_client.mdx`、`client/sse_client.mdx`、`client/ws_client.mdx` | 已回写（2026-11-28；本轮复核） |

| 27 | Agent Memory 会话隔离与清理 | 文档原先宣称默认会话隔离已生效，并将 `Memory.clear()` 描述为清理当前会话；当前实现中 InMemoryMemory 的检索跨所有 session 桶，VectorMemory 默认 sessionId 为空且 clear() 不删除持久化向量数据 | `InMemoryMemory#search/getRecent` 遍历全部 sessionMemories；`VectorMemory#search` 按 options.sessionId 过滤而写入缺失 ID 时填充 default；`VectorMemory#clear` 只清缓存，`clear(sessionId)` 才删除指定会话 | `ai/memory.mdx` | 文档已澄清；运行时代码问题待修复 |
| 28 | Embedding 向量维度 | 文档没有提示 embed 是同步阻塞调用，也没有提醒向量集合维度需匹配模型实际输出 | `EmbeddingModel.embed` 内部等待 HTTP Future；请求体当前硬编码 `dimensions=1`，而 `VectorMemoryOptions` 默认维度为 1024，需验证 Provider 实际行为 | `ai/embedding.mdx`、`ai/memory.mdx` | 文档已补充限制；运行时代码问题待修复 |
| 29 | Agent 状态终态 | Agent 示例打印 `getState()`，容易让开发者把状态值当作任务完成标志 | 当前 `ReActAgent` 只写入 RUNNING / TOOL_EXECUTION，没有写入 FINISHED / ERROR | `ai/agent.mdx` | 已从最小示例移除；运行时代码问题待修复 |

| 30 | 安全与静态资源示例 | WAF 的 HttpHandler 示例错误地使用 Context 字段；BasicAuth Router 示例调用不存在的 Context.write；文档未说明 WAF 根路径放行及静态资源 Handler 的异步入口限制 | HttpHandler 接收 HttpRequest；RouterHandler 接收 Context；WAF.checkUri 对 `/` 直接放行；HttpStaticResourceHandler 同步 handle 直接抛 UnsupportedOperationException，异步入口先 URL 解码再检查穿越片段 | `practices/security.mdx`、`practices/static-files.mdx` | 已回写（2026-11-28；本轮复核） |
## 已确认「不补」的

| 主题 | 结论 |
|---|---|
| Spring 兼容层（`@GetMapping`、`@RestController`、全局异常处理） | 不是缺口。缺这些是设计边界，`AGENTS.md` 的「会写错的地方」已列出替代写法 |

## 附录：一条命令列出编译期消费的全部配置键

改配置相关文档前先跑这个，比翻源码快：

```bash
grep -rnE 'JSONPath\.eval\(config' feat-cloud-aot/src/main/java \
  | sed 's/.*JSONPath.eval(config, //' | sed 's/).*//' | sort -u
```

输出即 `feat.yml` 里真正生效的键。没出现在这里的键，写进 `feat.yml` 不会报错，但也**不会生效**。
