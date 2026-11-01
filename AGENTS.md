# AGENTS.md — Feat

Feat 是一个 Java Web 服务框架（JDK 8+ / Maven）。写代码前先确认用哪一层：

| 层 | 启动入口 | 编程模型 |
|---|---|---|
| **Feat Core** | `Feat.httpServer().listen()` | 手写 Router / Handler，无注解、无容器 |
| **Feat Cloud** | `FeatCloud.cloudServer().listen()` | `@Controller`、`@Bean`，注解在**编译期**转成普通 Java 代码 |

Feat Cloud 的注解长得很像 Spring Boot，但**工作方式不同**：没有运行时注解扫描，没有反射装配，没有 Servlet 容器。路由和依赖注入是 `mvn compile` 阶段由注解处理器生成的。

> 如果你要**新建一个 Feat 项目**，直接跳到 [最小可运行工程](#最小可运行工程feat-cloud)。
> 如果你要**修改 Feat 本身**，看 [仓库结构与构建](#仓库结构与构建)。

## 会写错的地方

下面每一条都是 Spring 直觉在这里不成立的场景。按出现频率排序。

| # | 不要写（Spring 习惯） | 应该写 |
|---|---|---|
| 1 | `@GetMapping` / `@PostMapping` / `@PutMapping` / `@DeleteMapping` | 这些注解**不存在**。统一写 `@RequestMapping(value = "/x", method = RequestMethod.GET)` |
| 2 | `@PathVariable("id")` | `@PathParam("id")`，路径写 `/:id` 或 `/{id}`（两种等价） |
| 3 | `@RequestParam("q")` | `@Param("q")`。可能缺失的参数声明为包装类型 `Integer`，否则拆箱 NPE |
| 4 | `@Service` / `@Component` / `@Repository` / `@Configuration` | 全部用 `@Bean`。标在类上创建该 Bean，标在方法上把返回值交给容器 |
| 5 | `@RestController` | `@Controller`。Feat 不区分两者，返回 POJO 自动转 JSON |
| 6 | `HttpServletRequest` / `HttpServletResponse` / `jakarta.servlet.*` | 不存在。需要请求上下文时，方法参数注入 `tech.smartboot.feat.router.Context`，用 `context.Request` / `context.Response` |
| 7 | `ResponseEntity<T>` / `HttpStatus` | 不存在。直接返回对象走 JSON；需要统一响应体时返回 `RestResult.ok(data)` / `RestResult.fail(msg)` |
| 8 | `@ControllerAdvice` / `@ExceptionHandler` | 不存在（全仓库无此注解）。不要假设有全局异常处理链 |
| 9 | `@Value("app.name")` | 必须带占位符：`@Value("${app.name}")`。不带 `${}` 注入的是字面量 |
| 10 | `application.yml` / `application-prod.yml` | `feat.yml` / `feat-dev.yml` / `feat-prod.yml`，位于 `src/main/resources` |
| 11 | 类上 `@RequestMapping("/users")` + 方法 `@RequestMapping("/hello")` | 类级前缀写在 `@Controller("users")` 的值里，方法上只写 `@RequestMapping("/hello")` |
| 12 | 改完代码重跑 jar 就能生效 | **编译期生成**：改了 `@Controller` / `@Bean` / `@Autowired` / `@Value` / `feat.yml` 之后必须重新构建。只重启旧 jar 不会生效；只改 `feat.yml` 时 `mvn compile` 也可能被判为增量而跳过注解处理器，用 `mvn clean compile` 最稳 |
| 13 | `@Autowired private SqlSessionFactory sessionFactory;` 里字段随便起名 | **`@Autowired` 按字段名取 Bean，不看类型**。名字对不上不会注入成 null，而是启动时抛 `bean[xxx] not ready`。字段名必须是类名首字母小写，或者类上显式写 `@Bean("想要的name")`。典型翻车：`TxTemplate` 注入成 `tx` |

补充几个容易忽略的边界：

- `feat.yml` 的 `server:` 块只绑定 `int` 和 `String` 字段，`boolean` / `long` 写进去不生效。
- 环境名只能包含字母和数字（`dev`、`prod`、`gray1`），`feat-prod-us.yml` 不会被识别。
- 用 `maven-shade-plugin` 打 fat jar 时，必须用 `AppendingTransformer` 合并 `META-INF/services/tech.smartboot.feat.cloud.CloudService`，否则应用能启动但所有路由 404。
- **返回值序列化在编译期完成**，逐字段调 getter：没有对应字段的计算属性（如只在 `getHitRate()` 里算的值）不会被输出，接口里静默少一个键。要输出就落成真实字段。
- **DTO 嵌套超过两层会退化成 FastJSON2 序列化**：`RestResult → A → B → List<C>` 里的 `C` 走运行期序列化，而 `RestResult → A → List<C>` 里是逐字段写出。两条路径的字段集合可能不同——FastJSON2 默认省略值为 `null` 的字段，生成的代码会写出 `null`。表现是同一个 DTO 在两个接口上 JSON 形状不一致，客户端只对着其中一个写解析就出错。DTO 嵌套别超过两层；表达「没有值」用空串或空集合，别用 `null`。文档见 `cloud/response.mdx` 的同名 caution。
- **启动失败的异常会被解包到最底层 cause**（`FeatException#getRootCause`）。`new IllegalStateException(msg, e)` 里的 msg 不会出现在控制台，只剩 `Connection refused` 之类。要让提示可见，就把信息拼进自己的 message 且不设 cause。
- **注入的 Mapper 会自动提交事务。** 生成的 Mapper 包装类每个方法都 `openSession(true)`：一次业务动作要写两张表时，第一张已经提交、第二张失败，脏数据就留下了。要事务就在 `@Autowired SqlSessionFactory` 上 `openSession(false)`，事务体里全部改用 `session.getMapper(...)`（混用注入的 Mapper 等于把那一笔写扔出事务），最后必须 close。可运行的模板见 `demo/feat-plus/tx/TxTemplate`，文档见 `cloud/mybatis.mdx` 的「会话与事务」。

## 最小可运行工程（Feat Cloud）

`pom.xml`——**一个依赖就够**。`feat-cloud-starter` 会传递引入运行期的 `feat-cloud` 与编译期的 `feat-cloud-aot`（注解处理器），缺了它 Controller 不会被转成可加载的 `CloudService`：

```xml
<properties>
    <maven.compiler.source>8</maven.compiler.source>
    <maven.compiler.target>8</maven.compiler.target>
    <feat.version>2.4.2</feat.version>
</properties>

<dependencies>
    <dependency>
        <groupId>tech.smartboot.feat</groupId>
        <artifactId>feat-cloud-starter</artifactId>
        <version>${feat.version}</version>
    </dependency>
</dependencies>
```

> 想让运行产物更小，可以把 starter 标成 `provided`、再单独加一个运行期的 `feat-cloud`，
> 这样注解处理器的 jar 不会进最终包。代价是多一个依赖，且漏写 `provided` 或漏加 `feat-cloud` 都会出问题。
> 两种写法都能跑。`demo/helloworld` 用的是上面这种，新项目照抄上面这种即可。

`src/main/java/demo/Bootstrap.java`：

```java
package demo;

import tech.smartboot.feat.cloud.FeatCloud;
import tech.smartboot.feat.cloud.RestResult;
import tech.smartboot.feat.cloud.annotation.Controller;
import tech.smartboot.feat.cloud.annotation.PathParam;
import tech.smartboot.feat.cloud.annotation.RequestMapping;
import tech.smartboot.feat.cloud.annotation.RequestMethod;

@Controller("users")
public class Bootstrap {

    @RequestMapping(value = "/hello", method = RequestMethod.GET)
    public String hello() {
        return "hello Feat";
    }

    @RequestMapping(value = "/:id", method = RequestMethod.GET)
    public RestResult<String> getUser(@PathParam("id") String id) {
        return RestResult.ok("user " + id);
    }

    public static void main(String[] args) {
        FeatCloud.cloudServer().listen();
    }
}
```

改端口和包扫描写在入口，不写在 `application.properties`：

```java
FeatCloud.cloudServer(options -> {
    options.port(8080);
    options.setPackages("demo");   // 不在扫描范围内 = Bean 与路由都不会生成
}).listen();
```

启动成功时控制台会打印生成结果，这是判断"有没有生效"最快的证据：

```text
Feat Router:
 |-> /users/hello ==> Bootstrap@hello
 |-> /users/:id ==> Bootstrap@getUser
```

验证：

```bash
mvn -q compile
mvn -q dependency:build-classpath -Dmdep.outputFile=cp.txt
java -cp "target/classes:$(cat cp.txt)" demo.Bootstrap
curl http://localhost:8080/users/hello   # hello Feat
curl http://localhost:8080/users/42      # {"success":true,"code":200,...,"data":"user 42"}
```

注解处理器生成的代码在 `target/generated-sources/annotations/**/FeatApplication.java`。这个文件不存在，就说明 `feat-cloud-starter` 没生效或 IDE 没走 Maven 编译。

以上工程（JDK 8 + Feat 2.4.2，空 Maven 仓库从 Central 拉取）实测可编译、可启动、两个接口返回符合预期。改动本文件里的示例后请重新跑一遍。

## 校验清单

生成代码前自检，能挡掉大部分"编译过了但跑不起来"：

1. 有没有用到上表第 1–8 行的 Spring API？有就换掉。
2. 依赖里有没有 `feat-cloud-starter`（`provided` 也算）？
3. 必要参数用了包装类型还是基本类型？
4. 启动日志里出现了目标路由吗？没有就是没编译或包扫描没覆盖。
5. 改完注解或 `feat.yml` 后，重新跑了 `mvn compile` 吗？
6. 本次有没有遇到"文档没写清、得读源码才知道"的地方？有就去 `pages/DOCS_GAPS.md` 登记并回写文档——这是交付的一部分。

## 仓库结构与构建

```text
feat-core/            HTTP 服务器、Router、Context（Feat 的地基）
feat-cloud/           Feat Cloud 运行时：FeatCloud、RestResult、CloudOptions、Session
feat-cloud-aot/       注解定义 + 编译期注解处理器（@Controller/@Bean 在这里被翻译成代码）
feat-cloud-starter/   用户依赖的编译期入口，传递引入 aot 与 cloud
feat-ai/              LLM / MCP 相关能力
demo/                 helloworld、feat-plus、mybatis、nacos、benchmark 等可运行示例
pages/                文档站（Astro + Starlight），文档源在 pages/src/content/docs/
```

- 编译：JDK 8，`mvn -q -pl feat-core compile`
- `feat-core` 没有单元测试，行为验证靠 `demo/` 下的工程端到端跑。
- 文档不是 `docs/`，是 `pages/src/content/docs/**/*.mdx`。新增文档页要带 frontmatter（`title`、`description`、`sidebar.order`）。

## 工作方式：文档优先，缺口回写

用 Feat 开发时按这个顺序取证，不要跳步，也不要凭 Spring 印象猜：

1. **先查文档**。本地 `pages/src/content/docs/**/*.mdx` 与线上 `llms-full.txt` 是同一份内容。文档怎么写就怎么用。
2. **文档没覆盖、或对不上源码，再读源码**。以源码为准，不要以文档为准。
3. **把结论写回文档**。和代码改动放在同一次提交里，并在 [`pages/DOCS_GAPS.md`](./pages/DOCS_GAPS.md) 销账。

第 3 步是硬要求，不是"有空再补"。文档缺口出现过一次就不会只坑一个人——下一个人（或下一次的你自己）还会踩同一个。**只读源码不写回去，等于把这次的理解一次性消耗掉。**

需要回写的三种情形，都算「文档不满足」：

| 情形 | 实例 |
|---|---|
| 完全没讲 | 业务缓存、MyBatis 会话与事务 |
| 讲了但和源码不一致 | `feat.redis.host` / `port` —— 这两个键没有任何代码读取，实际只认 `feat.redis.address` |
| 讲了但漏了前提 | `session.store-type: redis` 要求同时存在 `feat.redis` 块，否则生成 `getBean("redisun")` 但 bean 没注册：编译通过、启动失败 |

### 三条最高杠杆的取证命令

比通读源码快，先跑这三个再决定要不要深入：

```bash
# 1. feat.yml 里哪些键真的生效（没出现在结果里的键写了也不报错、也不生效）
grep -rnE 'JSONPath\.eval\(config' feat-cloud-aot/src/main/java \
  | sed 's/.*JSONPath.eval(config, //' | sed 's/).*//' | sort -u

# 2. 某个注解在编译期被翻译成什么（改完跑一次 mvn compile 再读）
find target/generated-sources -name "FeatApplication*.java"

# 3. 某个特性有没有实现（先确认存在，再读实现）
grep -rlnE "关键词" --include=*.java feat-core/src feat-cloud/src feat-cloud-aot/src
```

### 工具坑

- **macOS 的 grep 是 BSD 版，`\|` 不支持**。要交替匹配必须加 `-E`，否则返回空结果——空结果会被误读成"框架没这个功能"。这条已经骗过一次（搜"缓存"零命中，实际文档里就有）。
- 搜文档正文用 `grep -rniE`，别用 `grep "a\|b"`。

### 文档索引

- 文档索引（llms.txt）：https://smartboot.tech/feat/llms.txt
- 全量文档（llms-full.txt）：https://smartboot.tech/feat/llms-full.txt
- Controller：https://smartboot.tech/feat/cloud/controller/
- Bean 与依赖注入：https://smartboot.tech/feat/cloud/bean/
- 方法拦截器：https://smartboot.tech/feat/cloud/interceptor/
- Session 与 Redis 会话：https://smartboot.tech/feat/cloud/session/
- 业务缓存：https://smartboot.tech/feat/cloud/cache/
- 从 Spring Boot 迁移：https://smartboot.tech/feat/practices/from-spring/
- 报错排查：https://smartboot.tech/feat/cloud/troubleshooting/
- Feat Core 路由：https://smartboot.tech/feat/server/router/

## 修改 Feat 本身时的约束

- **不要给 Feat 加 Spring 兼容层**。缺哪个 Spring API 不是 bug，是设计边界；需要兼容性讨论时先提 issue。
- **不要引入运行时反射扫描**。装配和路由都在编译期完成，这条是 Feat 的核心前提。
- **不要放宽 Java 8 约束**。不要用 `var`、`String.isBlank`、`List.of`、`record`。
- 提交信息遵循 Conventional Commits（`feat(scope):` / `fix(scope):` / `docs(scope):`）。

## 授权

仓库 LICENSE 为 Apache 2.0，全部模块代码均可自由用于商业场景；各模块 pom 的头部注释也是这个口径（"未经特别许可，需遵循 Apache-2.0 合理合法使用"）。
Feat Cloud 的持续演进依赖赞助者支持，商业项目采用前可了解当前支持模式：https://smartboot.tech/feat/sponsors/

**不要在任何材料里写出"需要授权才能使用""需获取赞助授权"这类结论。** 它与 LICENSE 冲突，且会让企业评估者当场放弃。对外口径以文档站 `pages/src/content/docs/sponsors.mdx` 为准。
