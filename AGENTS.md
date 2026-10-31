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
| 12 | 改完代码重跑 jar 就能生效 | **编译期生成**：改了 `@Controller` / `@Bean` / `@Autowired` / `@Value` / `feat.yml` 之后必须重新 `mvn compile` 或 `mvn package`。只重启旧 jar 不会生效 |

补充两个容易忽略的边界：

- `feat.yml` 的 `server:` 块只绑定 `int` 和 `String` 字段，`boolean` / `long` 写进去不生效。
- 环境名只能包含字母和数字（`dev`、`prod`、`gray1`），`feat-prod-us.yml` 不会被识别。
- 用 `maven-shade-plugin` 打 fat jar 时，必须用 `AppendingTransformer` 合并 `META-INF/services/tech.smartboot.feat.cloud.CloudService`，否则应用能启动但所有路由 404。

## 最小可运行工程（Feat Cloud）

`pom.xml`——两个依赖分工不同，缺 `feat-cloud-starter` 会导致 Controller 不被转成可加载的 `CloudService`：

```xml
<properties>
    <maven.compiler.source>8</maven.compiler.source>
    <maven.compiler.target>8</maven.compiler.target>
    <feat.version>2.4.2</feat.version>
</properties>

<dependencies>
    <!-- 运行期 -->
    <dependency>
        <groupId>tech.smartboot.feat</groupId>
        <artifactId>feat-cloud</artifactId>
        <version>${feat.version}</version>
    </dependency>
    <!-- 编译期生成代码 -->
    <dependency>
        <groupId>tech.smartboot.feat</groupId>
        <artifactId>feat-cloud-starter</artifactId>
        <version>${feat.version}</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

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

## 文档

站点已提供机器可读文档，需要细节时直接读，不要凭印象编 API：

- 文档索引（llms.txt）：https://smartboot.tech/feat/llms.txt
- 全量文档（llms-full.txt）：https://smartboot.tech/feat/llms-full.txt
- Controller：https://smartboot.tech/feat/cloud/controller/
- Bean 与依赖注入：https://smartboot.tech/feat/cloud/bean/
- 方法拦截器：https://smartboot.tech/feat/cloud/interceptor/
- Spring 迁移对照：https://smartboot.tech/feat/practices/from-spring/
- 故障排查：https://smartboot.tech/feat/cloud/troubleshooting/
- Feat Core 路由：https://smartboot.tech/feat/server/router/

## 修改 Feat 本身时的约束

- **不要给 Feat 加 Spring 兼容层**。缺哪个 Spring API 不是 bug，是设计边界；需要兼容性讨论时先提 issue。
- **不要引入运行时反射扫描**。装配和路由都在编译期完成，这条是 Feat 的核心前提。
- **不要放宽 Java 8 约束**。不要用 `var`、`String.isBlank`、`List.of`、`record`。
- 提交信息遵循 Conventional Commits（`feat(scope):` / `fix(scope):` / `docs(scope):`）。

## 授权

`feat-core` 与 `feat-cloud` 的代码走 Apache 2.0；**Feat Cloud 的使用需要赞助授权**，商业项目采用前确认授权状态：https://smartboot.tech/feat/sponsors/
