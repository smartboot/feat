# 文档缺口台账

用 Feat 写业务时撞到的文档问题，逐条登记、回写后销账。

与 [`DOCS_REVIEW.md`](./DOCS_REVIEW.md) 的分工：那份是**通读评审**（一次看完全部 36 篇，找出过期、矛盾、断链）；这份是**开发过程中实际撞到的缺口**——按代码写不动、写错了才记下来，因为"实际卡住过"比"看起来可以更好"更能说明文档该补什么。

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
| 10 | `appendix/aot.mdx` | 整页约一半在讲已删除的 `feat-cloud-aot-vm` 模块 | 仓库无此模块，`version_history` 记 v2.1.0 已移除 | 重写该页 | 待回写（见 DOCS_REVIEW H1） |

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
