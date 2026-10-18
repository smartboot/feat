# Feat AI Gateway 数据面设计（v0.1）

> 本文是 `demo/feat-ai-gateway` 的**实现依据**，不是产品规划。产品层面的判断见仓库根目录 `AI_GATEWAY_DESIGN.md`。
>
> **变更流程：任何设计调整先改本文，再改代码。** 代码与本文不一致时，以本文为准，或先更新本文再改代码。

---

## 1. 定位与边界

一句话：**OpenAI 协议兼容的 LLM 网关，只做数据面。**

| | 数据面（本项目） | 控制面（后续） |
|---|---|---|
| 职责 | 鉴权、路由、限流、转发、计量 | 配置编辑、密钥托管、用量报表、UI |
| 状态 | 无状态，配置常驻内存 | 有状态，需要 DB |
| 故障影响 | 挂了就是事故 | 挂了不影响流量 |
| 交付形态 | 一个 jar | 一个 Web 应用 |

铁律：**数据面不得依赖控制面在线。** 配置只有一个入口 —— `applyConfig(snapshot)`，整份替换。文件、watch、将来的控制面推送、admin API 走同一条路。

---

## 2. 技术选型：为什么用 Feat Cloud

| 部分 | 用什么 | 理由 |
|---|---|---|
| 进程与路由 | **Feat Cloud**（`FeatCloud.cloudServer` + `@Controller` / `@RequestMapping`） | 管理面用注解声明路由、Bean 注入、异步响应都是现成的；数据面因为要连接生命周期，走 `router.route`（ADR-8） |
| 转发核心 | feat-core 的 `HttpClient` + `Stream` | 需要逐字节透传，不能经过任何序列化层 |
| 配置 | YAML + SnakeYAML | 人可读、可 diff、可被控制面渲染 |
| JSON | fastjson2（feat 自带） | 不额外引入依赖 |

**Feat Cloud 提供什么：** 控制器注册、方法参数注入（`HttpRequest` / `HttpResponse` / `Context`）、异步响应约定、Bean 管理。
**Feat Cloud 不提供什么：** 流式转发的具体写法。所以 `proxy` / `policy` / `metric` 三个包不依赖 Feat Cloud，只依赖快照对象和 feat-core。

### 2.1 控制器契约（实现时踩过的点）

1. **同步方法的响应会在方法返回后关闭连接。** 生成代码给非 `AsyncResponse` 返回值的控制器只实现同步 `handle(ctx)`，再由 `RouterHandler` 的默认方法补一句 `completableFuture.complete(null)`。框架看到 future 已完成就当响应结束，`keepConnection` 不成立就关连接。所以流式接口必须返回 `AsyncResponse`，由代理在流结束时 `complete()`。
2. `AsyncResponse.complete(null)` 在处理链里等价于"只完成 future，不写任何 body"（`gzipResponse` 拿到 `null` 直接 return），正好适合"body 已经由代理自己写完了"的场景。**这是流水线接口能走 `@Controller` 的唯一缝隙。**
3. `@RequestMapping` 是**编译期注解**（`RetentionPolicy.SOURCE`），路由由 `feat-cloud-aot` 的注解处理器在编译时生成，处理器还会重写 `META-INF/services/tech.smartboot.feat.cloud.CloudService`。新增控制器必须重新编译，不能靠运行时扫描；`feat.yaml` 也必须在编译期就能读到。
4. **`@Controller` 覆盖不了 `onClose`。** 生成的路由处理器是只实现 `handle()` 的匿名 `RouterHandler`，`onClose` 走接口默认空实现。而 `Router.onHeaderComplete` 会把 `HttpEndpoint#serverHandler` 从 `Router` 换成命中的路由处理器，`SESSION_CLOSED` 时的 `onClose` 就再也回不到外层。详见 ADR-8 —— 结论是**数据面主路径不能用 `@Controller`**。
5. `FeatCloud.cloudServer()` 内部已经调用过 `server.httpHandler(router)`，而 `HttpMessageProcessor#httpServerHandler` 二次设置会抛 `IllegalStateException`。所以"先 cloudServer 再包一层 handler"这条路走不通，只能在 `Consumer<CloudOptions>` 里通过 `opt.getRouter()` 提前注册路由。
6. 请求体要非阻塞读，用 `AsyncBodyReadUpgrade`（`request.setUpgrade(...)`）把 body 先异步攒进缓冲区，再由 handler 从内存读；`AbstractCloudService#bodyAsyncRead` 做的就是这件事，但它只管 `@Param`/结构体参数的控制器，自己写 `RouterHandler` 时要手动调。

### 2.2 装配方式

```java
CloudOptions[] holder = new CloudOptions[1];
HttpServer server = FeatCloud.cloudServer(opt -> {
    holder[0] = opt;
    opt.port(config.port());
    opt.setPackages("tech.smartboot.feat.gateway");   // 只认本工程的 @Controller
    opt.registerBean("chatProxy", proxy);             // 外部 Bean，供 @Autowired 按字段名注入
    opt.getRouter().route(PATH_CHAT, "POST", new ChatRoute(proxy));  // 数据面，见 ADR-8
    opt.getRouter().route("/v1/*", notFound);         // 兜底 404，保持 OpenAI 错误结构
});
```

`@Controller` 侧用 `@Autowired` 注入（生成代码按**字段名**取 Bean：有同名 setter 就直接赋值，没有就反射写字段）。管理面全部走注解，数据面主路径走 `router.route`。

---

## 3. 模块结构

```
demo/feat-ai-gateway/
  src/main/java/tech/smartboot/feat/gateway/
    GatewayServer.java     装配入口：FeatCloud.cloudServer + 外部 Bean + 数据面路由
    controller/   Feat Cloud 控制器（管理接口 + /v1/models）
    proxy/        上游连接池、SSE 透传、数据面路由（ChatRoute）、错误响应
    config/       快照模型、YAML 解析、校验、热加载
    policy/       鉴权、限流、配额
    metric/       用量日志、Prometheus 指标
    mock/         假上游（联调与压测用）
  src/main/resources/feat.yaml   编译期被注解处理器读取
  bench/          P0 压测脚本（仅标准库）
  scripts/        冒烟脚本
  gateway.yaml    本地联调配置（运行时读，和 feat.yaml 是两回事）
  gateway.example.yaml  生产形态示例
```

`gateway.yaml` 是网关自己的业务配置（provider / model / key），`feat.yaml` 是 feat 框架配置（端口、gzip、静态资源）。两者不合并：业务配置要走 `applyConfig(snapshot)` 单一入口，不能被框架的配置加载器绕过。

分层约束：**`proxy` / `policy` / `metric` 只依赖快照对象，不依赖配置从哪来，也不依赖 Feat Cloud。** `ChatRoute` 虽然是被 Router 调用的 `RouterHandler`，但它只碰 `HttpRequest`/`HttpResponse` 和 feat-core，不 import 任何 `tech.smartboot.feat.cloud.*`。
守住这条，以后加控制面时这三个包一行不用改。

---

## 4. 配置模型

### 4.1 快照 + 版本

```java
applyConfig(snapshot)   // 整份替换，版本号单调递增
```

不用增量命令（`addProvider` / `removeKey`）。原因：增量模型下控制面要自己解决"一次 UI 操作翻译成一串命令、失败怎么回滚、多实例怎么保证顺序"，这些都会变成以后的债。

热加载规则：
- 轮询文件 mtime，解析 + 校验**全部通过**才原子替换；
- 任何一项失败：保留旧快照，打 WARN，**不中断服务**；
- 快照不可变，避免并发读写撕裂。

### 4.2 YAML 字段

| 段 | 字段 | 默认 | 说明 |
|---|---|---|---|
| `version` | 整数 | 1 | 配置版本，admin 的 `If-Match` 用它做乐观锁 |
| `server` | `port` | 8080 | 监听端口 |
| | `idleTimeoutMs` | 300000 | 下游连接最长静默。低于上游最长静默时间会切断长回答 |
| `admin` | `token` | 空 | 空 = 只允许本机访问 `/admin/*` 与 `/metrics` |
| `log` | `dir` | logs | usage 日志目录 |
| | `usageEnabled` | true | 关掉只影响落盘，不影响指标 |
| `providers[]` | `name` / `baseUrl` / `apiKey` | — | `apiKey` 支持 `${ENV}`，解析不到直接报配置错误 |
| | `maxConnections` | 1000 | **必须 ≥ 目标并发**，见 §7 |
| | `connectTimeoutMs` | 3000 | 同时是"连接池申请超时" |
| | `idleTimeoutMs` | 300000 | 上游连接空闲回收 |
| | `authHeader` / `authPrefix` | Authorization / `Bearer ` | 兼容非标准鉴权头 |
| `models[]` | `name` / `provider` / `upstream` / `priority` | 0 | 同名多条 = 按 priority 降级 |
| `keys[]` | `id` / `keyHash` | — | 只存 sha256 前 16 位 |
| | `models` | 空=全部 | 该 key 可调用的模型白名单 |
| | `rpm` / `tpm` / `monthlyTokens` | 0=不限 | 限额 |

---

## 5. 请求链路

```
客户端
  │  POST /v1/chat/completions
  ▼
Router (Feat Cloud 生成)
  │  onHeaderComplete 命中 ChatRoute -> AsyncBodyReadUpgrade（body 异步攒进缓冲）
  ▼
ChatRoute (RouterHandler，数据面)
  │  鉴权：Bearer -> sha256 前缀比对（纯内存）
  │  失败：OpenAI 错误结构 + completeAsync
  ▼
ChatProxy
  │  解析 body -> model -> 路由列表（priority 升序）
  │  限流：RPM/TPM/月度配额
  │  注入 stream_options.include_usage
  ▼
UpstreamPool.acquire(provider)   ← 按 fingerprint 复用 HttpClient
  │
  ▼  feat-core HttpClient.post().onResponseHeader/onResponseBody(Stream)
上游 Provider
  │  第一个 chunk 到达
  ▼
逐 chunk 写下游 + flush  → 客户端
  │
  ├─ 结束：解析 usage -> 计量 -> 异步落盘 -> 释放租约 -> future.complete(null)
  └─ 客户端断开：RouterHandler#onClose -> ChatProxy#onConnectionClose -> 取消上游
```

四条不能妥协的：

1. **零缓冲**：上游每个 chunk 立刻写下游并 flush。任何"先收完再发"都会让首字延迟从 300ms 变成 8s。
2. **客户端断开 → 取消上游**：靠 `HttpHandler#onClose` 把"连接断了"翻译成 `HttpRest.close()`。不做这一步，用户关页面后上游还会把整段回答生成完。
3. **失败降级**：仅在**尚未向客户端提交响应头**时降级；已提交就只能断开，不能改状态码。
4. **计量异步**：请求线程只入队，落盘在独立线程，队列满则丢弃并计数，绝不反压转发。

---

## 6. 接口契约

### 6.1 兼容端点

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/v1/chat/completions` | 支持 `stream`，自动注入 `include_usage` |
| POST | `/v1/completions` | 同上（旧协议） |
| POST | `/v1/embeddings` | 透传 |
| GET | `/v1/models` | 返回配置里声明的模型名 |

错误统一为 OpenAI 结构：`{"error":{"message","type","code"}}`，现成 SDK 能直接解析。

### 6.2 管理接口（未来控制面的后端）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/admin/config` | 当前快照（密钥打码）+ 版本 |
| POST | `/admin/config` | 提交新快照（YAML），`If-Match: <version>` 做乐观锁，冲突返回 409 |
| POST | `/admin/reload` | 从文件重载 |
| GET | `/admin/stats` | 运行时统计 |
| GET | `/metrics` | Prometheus 文本 |
| GET | `/healthz` | 存活探针 |

`POST /admin/config` 与"改文件重载"语义完全相同 —— 控制面只是在替用户写这份 YAML，没有第二套配置逻辑。

---

## 7. 关键决策记录（ADR）

### ADR-1：配置用快照，不用增量命令
见 §4.1。

### ADR-2：上游客户端共享一个 I/O 线程组（P0 压出来）
feat 的 `HttpClient` 默认自建 `AsynchronousChannelGroup`，等于**每条上游连接一组线程**。1000 并发直接 `unable to create new native thread`。

改为所有 provider 共享一个 group（线程数 = `max(4, cores*2)`）后，连接数可以远大于线程数。

**结论：这是 AIO 客户端的默认姿势问题，不是网关独有。** 任何用 feat 做高并发代理/爬虫的项目都会踩到。

### ADR-3：流式接口必须走异步响应（P0 压出来）
feat 的 `HttpHandler` 在方法返回后会检查 `CompletableFuture`：已完成 → 认为响应结束，`keepConnection` 失败就关连接。所以：

- 同步写法（返回 `void`，稍后写 body）一定拿不到数据；
- 必须用异步签名，并在**每条终止路径**（正常结束 / 上游失败 / 客户端断开）都 `complete`，否则连接和请求对象都回收不掉。

### ADR-4：流式计量的 usage 必须主动要
OpenAI 流式响应默认不返回 `usage`。网关在请求里注入 `stream_options.include_usage=true`；拿不到就按字节估算并把 `estimated=true` 写进日志 —— **不要假装精确，客户对账时会对出来**。

### ADR-5：中断请求也要计费
客户端断开时按已输出字节估算完成 token，记 `status=499`。否则"用户关页面"会被记成 0 消耗，成本核算永远是错的。

### ADR-6：限流先做单机
RPM/TPM 是进程内滑动窗口。多副本时 N 个实例 = N 倍配额，这个问题 etcd 解决不了（写入吞吐不适合按请求记账），要用 Redis 或配额租借，属于云原生版的事。**现在只在文档和日志里标注，不提前抽象。**

### ADR-7：TTFT 只统计流式请求
非流式请求没有"首字"概念，把它算进 TTFT 会把指标带偏。

### ADR-8：数据面主路径不能用 `@Controller`
"客户端断开 → 取消上游"是数据面的铁律（§5 第 2 条），它依赖 `onClose`。而框架的派发链路是：

```
SESSION_CLOSED -> request.getServerHandler().onClose(request)
```

`Router.onHeaderComplete` 在匹配到路由后执行 `request.setServerHandler(命中的处理器)`，于是 `onClose` 落到**生成代码里的匿名 `RouterHandler`** 上——它只实现了 `handle()`，`onClose` 走接口默认空实现。同时：

- `Router` 是 `final`，不能覆写 `onHeaderComplete` 阻止这次替换；
- `Router` 里的 `RouterHandlerImpl` 是包级私有，拿不到；
- `Interceptor` 没有 `onClose`；
- `FeatCloud.cloudServer()` 已经把 `Router` 设成 server handler，`httpHandler()` 二次调用抛 `IllegalStateException`；
- `ApplicationContext` 构造器是包级私有，绕开 `FeatCloud` 自己装配也不行。

唯一还留着 `onClose` 覆写点的是 **`RouterHandler` 接口**（`default void onClose(HttpEndpoint)`）。所以：

- **数据面主路径**（`/v1/chat/completions` 等）用 `router.route(path, "POST", new ChatRoute(...))` 手工注册，`ChatRoute` 覆写 `onClose`；
- **管理面**（纯请求-响应、不需要连接生命周期）用 `@Controller` + `@RequestMapping`。

判断标准不是"哪边代码多"，而是**这个接口需不需要知道连接什么时候断**。

> 这条也是给 feat 的反馈：`@Controller` 目前表达不了"连接关闭"语义。将来可以在 `@RequestMapping` 上加一个 `onClose` 回调方法名，或让 `RootHandler` 暴露一个可插拔的 close hook，数据面就能完全注解化。

### ADR-9：请求体异步读，不占网络线程
`AsyncBodyReadUpgrade` 把 body 先攒进缓冲区（`ByteBuffer`，大于读缓冲时才分配），攒满后 `setInputStream` 并从内存读。用它之后控制器里读 body 不再是 socket 读，不会阻塞 I/O 线程。前提是请求带 `Content-Length`；chunked 请求没有长度，只能退回同步读（现状：OpenAI SDK 都会带长度）。

---

## 8. 验收标准与实测

环境：macOS / 4 核 / JDK 8 / 网关 `-Xms256m -Xmx512m`；mock 上游与网关同机（因此延迟含同机资源竞争）。
压测脚本：`bench/sse_bench.py`（仅标准库，端到端测量，含网关开销）。

| 场景 | 并发 | 成功 | heap 峰值 | TTFT p50 | 说明 |
|---|---|---|---|---|---|
| 单连接 | 1 | 20/20 | — | 115 ms | mock 自身 100ms，网关开销 ≈15ms |
| 中等并发 | 20 | 200/200 | — | 119 ms | 基本线性，无排队 |
| 高并发 | 1000 | **998/1000** | 64 MB（基线 19.7MB，+44.3MB） | 2752 ms | 同场景直连 mock 基线 1869 ms |

对照：修复 ADR-2 之前，同样 1000 并发有 **458/1000 失败**，错误为 `unable to create new native thread`。修复后失败 2 条，均为 mock 侧 `Connection reset by peer`。

判定：
- **连接数**：1000 并发流式连接可以稳定建立并跑完 ✅
- **内存**：峰值增长 < 50MB ✅（但这是在 512MB 堆上限下的采样峰值，不是长期稳态）
- **TTFT 增量**：同机同 mock 下 +883ms（p50），其中大部分是 4 核上三个 JVM 抢 CPU，**不能当作网关真实开销**；单连接口径的 +15ms 更能代表网关自身成本 ⚠️
- **连接归零**：跑完后 `inflight=0` ✅（基线脚本在过载时偶发抓不到指标，需重试）

> 结论：P0 的目标（能不能扛住 1000 并发流式）达成了；但**"TTFT 增加 < 10ms"这个验收标准在 4 核同机环境下无法验证**，需要一台独立压测机才有意义。这条必须如实记录，不能拿 2752ms 当成绩。

---

## 9. 已知限制（按优先级）

| # | 限制 | 影响 | 计划 |
|---|---|---|---|
| 1 | chunked 请求体只能同步读 | 无 `Content-Length` 的 body 会短暂占用网络线程 | P2：给 `Upgrade` 加长度未知的分片模式 |
| 2 | 无背压 | 客户端读得慢时下游缓冲增长 | P1：按写缓冲水位丢弃/断开 |
| 3 | 单机限流 | 多副本配额翻倍 | 云原生版：Redis / 配额租借 |
| 4 | 无 TLS 终止 | 需要前置 nginx/网关 | 交给部署层，不在数据面做 |
| 5 | 无 Key 热更新审计 | 改配置无操作记录 | 控制面阶段补充 |
| 6 | usage 日志按天单文件 | 量大时无轮转 | P2：按大小轮转 + 压缩 |
| 7 | 数据面主路径脱离注解 | 路由分散在 `GatewayServer` 与 `@Controller` 两处 | 等 feat 支持 close hook 后迁回 `@Controller`（ADR-8） |

---

## 10. 后续演进（只记方向，不提前实现）

- **P1**：配置抽象成 `ConfigSource` 接口（file 一个实现），为 etcd/Nacos 留口子；写缓冲背压。
- **P2**：多租户（Key 从静态配置变成动态资源）、租户级报表、计费出账；chunked body 的异步读。
- **P3**：控制面（配置编辑 + 用量看板），后端就是 §6.2 这几个接口。

**留接口叫设计，提前写三个实现叫过度设计。** 等真有客户问"能不能接 Nacos"，再花两天写。
