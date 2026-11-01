# Feat Plus

一个用 Feat Cloud 写成的用户管理后台，用来演示「编译期 AOT + 无 Servlet 容器」能撑起一个真实形态的业务系统：
登录鉴权、用户增删改查、分页筛选、多角色与数据权限、部门树、仪表盘统计、站内通知、演示数据重置，
前端是一个完整的 React 单页应用。菜单与按钮按当前账号的权限点显示，后端每个接口独立校验。

角色是**多选**的：一个人可以同时是部门主管和审计员，权限点取并集、数据范围取最宽；
数据范围决定「能对谁做」，并且落在每个按 id 的读写上——只过滤列表挡不住改 URL 猜 id。
后续规划见 [ROADMAP.md](ROADMAP.md)。

- 后端：Feat Cloud + MyBatis + H2（内存库，重启即重置）
- 前端：React 18 + Vite + shadcn/ui + TanStack Query
- 运行时：JDK 8

## 跑起来

```bash
# 1. 构建前端产物并放进静态资源目录
cd web && yarn install && yarn build
cd .. && cp -r web/dist/. src/main/resources/static/

# 2. 编译（注解处理器生成路由与装配代码，必须执行）
mvn -q compile

# 3. 启动
mvn -q exec:java
```

打开 http://localhost:8080 即可。演示账号：**admin / admin123**。

> 第 1 步可以跳过：没有前端产物时服务照常启动，API 可用，首页会提示如何构建。

## 接口

所有接口以 `RestResult` 信封返回：

```json
{"success":true,"code":200,"message":null,"data":{}}
```

除登录外均需 `Authorization: Bearer <token>`，未登录返回 **401**，前端据此清理本地令牌。
已登录但当前角色缺少对应权限点时返回 **403**，界面不会显示该操作，接口也不会执行。

| 方法 | 路径 | 说明 | 权限点 |
|---|---|---|---|
| POST | `/api/auth/login` | 登录，返回 accessToken（默认 7200 秒） | — |
| GET | `/api/auth/me` | 当前用户，含角色名与权限点 | — |
| POST | `/api/auth/logout` | 注销令牌 | — |
| GET | `/api/profile` | 我的资料（昵称、邮箱、部门、角色、数据范围） | — |
| PUT | `/api/profile` | 改自己的昵称与邮箱 | — |
| POST | `/api/profile/password` | 改自己的密码，返回被踢掉的会话数 | — |
| GET | `/api/users` | 分页 + 关键字/状态/角色/部门筛选 | `user:read` |
| GET | `/api/users/all` | 全量列表（下拉选项用） | `user:read` |
| GET | `/api/users/{id}` | 详情 | `user:read` |
| POST | `/api/users` | 创建 | `user:write` |
| PUT | `/api/users/{id}` | 更新 | `user:write` |
| POST | `/api/users/{id}/status` | 启用 / 停用 | `user:write` |
| POST | `/api/users/{id}/reset-password` | 重置密码，返回临时密码 | `user:write` |
| DELETE | `/api/users/{id}` | 删除 | `user:write` |
| GET | `/api/roles` | 角色列表（含权限点与在用人数字） | `role:read` |
| POST | `/api/roles` | 新建角色 | `role:write` |
| PUT | `/api/roles/{id}` | 更新角色 | `role:write` |
| POST | `/api/roles/{id}/status` | 启用 / 停用角色 | `role:write` |
| DELETE | `/api/roles/{id}` | 删除角色（在用 / 内置不可删） | `role:write` |
| GET | `/api/departments` | 部门树（每级带直属成员数） | `dept:read` |
| POST | `/api/departments` | 新建部门 | `dept:write` |
| PUT | `/api/departments/{id}` | 更新部门 / 调整上级 | `dept:write` |
| DELETE | `/api/departments/{id}` | 删除部门（有子部门或成员时拒绝） | `dept:write` |
| GET | `/api/dashboard` | 用户统计、登录趋势、最近动态 | `dashboard:read` |
| GET | `/api/activities` | 审计日志分页，支持类型 / 操作人 / 时间范围筛选 | `audit:read` |
| GET | `/api/activities/operators` | 日志里出现过的操作人（筛选下拉用） | `audit:read` |
| GET | `/api/notifications` | 通知列表（按当前登录人过滤） | — |
| POST | `/api/notifications/{id}/read` | 单条已读 | — |
| POST | `/api/notifications/read-all` | 全部已读 | — |
| POST | `/api/admin/reset-demo` | 恢复演示数据 | `system:write` |
| GET | `/api/system/cache` | 当前缓存后端与命中计数 | `system:write` |
| GET | `/api/system/selftest` | 地基自检（事务回滚是否真的生效） | `system:write` |

「权限」一栏为 `—` 的接口只要求登录。个人中心的三个接口刻意不挂权限点：
改自己的昵称和密码是基本权利，要求 `user:write` 会让普通成员永远改不了密码，
只能找管理员重置——那正是这组接口要修掉的缺陷。

权限点是固定的 9 个，定义在 `common/Permission` 与前端 `lib/permissions.ts`，两边必须一致：
`dashboard:read`、`user:read`、`user:write`、`role:read`、`role:write`、`dept:read`、`dept:write`、
`audit:read`、`system:write`。
角色就是把一组权限点打包；内置的 `ADMIN` 拥有全部权限且不可停用、不可改权限，是系统的兜底通道，
`AUDITOR` 是只读角色（含 `audit:read`），`DEPT_MANAGER` 是数据范围受限的角色（见下一节）。

### 多角色与数据权限

权限点（`permissions`）决定「能不能做」，数据范围（`department_scope`）决定「能对谁做」，两者正交。

- 关系表：`sys_user_role`，一个账号可以挂多个角色；
- 合并规则：权限点取**并集**，数据范围取**最宽**（「仅本人」+「本部门」→ 按本部门算）。
  范围收紧会让人干不了活，放宽只是多看到几个同事，所以取宽；
- 五个档位：`1` 全部 / `2` 自定义部门（配在 `sys_role_dept`）/ `3` 本部门 / `4` 本部门及以下 / `5` 仅本人；
- 落地位置：列表查询把可见部门翻译成 SQL 条件；**每个按 id 的读写还要单独校验目标是否在范围内**，
  否则改一下 URL 里的 id 就能越权。越权访问返回 404 而不是 403——不告诉调用方「这个 id 存在但你看不到」；
- 首页统计走同一套口径：能看 2 个人却在首页看到 12 人，用户只会以为权限坏了；
- **审计日志与首页近期动态也走数据范围**，按**操作人所属部门**过滤，不是按被操作对象。
  选操作人的理由：每条日志必然有操作人，而目标是可空的（登录、备份这类记录没有目标用户），
  按目标过滤会让这些记录在受限视图里凭空消失；一条日志也只属于一个部门，不存在归属歧义。
  代价是部门主管看不到「别的部门改了我的人」，这是范围模型的固有取舍；
- `ADMIN` 恒定全部数据，即使角色的 `department_scope` 被改坏也不会把管理员锁在门外。

权限点与数据范围是两道独立的门：只把 `audit:read` 授给一个「本部门」范围的角色，
他看到的日志也只有本部门那些。**只做列表过滤、忘了按 id 校验或忘了日志这类旁路接口，
等于这道门只装了一半。**

演示账号里 `alice` 挂了 `USER` + `DEPT_MANAGER`，登录后只能看到「产品设计部」的 2 个人，
侧边栏头像下拉会显示她当前的数据范围。

业务约束：用户名/邮箱唯一、不能停用或删除当前登录账号、必须保留至少一个已启用的管理员、
角色与部门必须是存在且启用的记录、部门不能挂到自己的子树里、删除部门前必须先清空子部门与成员。
按部门筛选时默认包含全部下级——选「技术中心」能看到「平台研发部 > 前端组」里的人。

**审计日志只读**：`GET /api/activities` 没有任何对应的写接口，日志只能由 `ActivityRecorder` 在业务操作时落库。
时间筛选按自然日理解，`to=2026-08-02` 时 8 月 2 日当天的记录也算在内。

**通知按人投递**：`sys_notification.user_id` 是接收人，NULL / 0 表示广播，大于 0 表示只投给该用户。
已读状态不在通知表上，而是每人一行记在 `sys_notification_read` ——放在通知表里用单列 `read_flag` 时，
广播通知只有一行，两个管理员谁点了已读，另一个人那边也会变成已读。

## 表结构约定

九张表（`sys_user`、`sys_role`、`sys_department`、`sys_user_role`、`sys_role_dept`、`sys_activity`、`sys_notification`、`sys_notification_read`、`sys_login_stat`）都带同一组审计字段：

| 列 | 类型 | 说明 |
|---|---|---|
| `is_deleted` | SMALLINT | 软删除标记，0 未删除 / 1 已删除 |
| `created_time` | TIMESTAMP | 创建时间 |
| `edit_time` | TIMESTAMP | 最后修改时间 |
| `creator` | BIGINT | 创建人的 `sys_user.id`，系统写入时留 NULL |
| `editor` | BIGINT | 最后修改人的 `sys_user.id`，系统写入时留 NULL |

**软删除是真生效的**：`DELETE /api/users/{id}` 只把 `is_deleted` 置 1，行保留在表里。所有查询、登录校验、唯一性检查都带 `is_deleted = 0`，所以删掉一个账号之后，同名账号可以重新创建。

> 代价要说清楚：`sys_user.username` 上原来的 `UNIQUE` 约束必须去掉——软删除后同名行仍在表中，唯一索引会让「删了再建」直接失败。唯一性改由 `UserService.assertUnique` 在应用层保证，比较范围是未删除的数据。高并发下仍可能插入重名，演示项目可以接受；生产环境更适合给 `(username, is_deleted)` 建唯一索引（删除时把 username 改写为 `name#id`），或引入独立的删除审计表。

`reset-demo` 是唯一例外：它用 `TRUNCATE ... RESTART IDENTITY` 真清空并重置自增，所以重置后
用户 id 恒为 1–12、角色 1–4、部门 1–10、通知 1–3，不会像普通删除那样留下软删行。

## 回归脚本与 CI

`scripts/` 下六份用 urllib 写的断言脚本，覆盖 255 项。用 urllib 而不是 curl，是为了对 JSON 做精确断言——
用 `grep -o '"code"'` 数条数会把 `RestResult` 信封里的 `code: 200` 也数进去。

```bash
# 先启动服务（见上面「跑起来」），然后：
python3 scripts/regress_crud.py     # 54 项：用户 / 角色 / 部门的基础读写与唯一性、软删除语义
python3 scripts/regress_scope.py    # 80 项：多角色并集、范围过滤、按 id 越权、审计日志的范围收窄
python3 scripts/regress_audit.py    # 44 项：审计日志筛选、通知按人投递与已读隔离
python3 scripts/regress_profile.py  # 40 项：个人中心改资料 / 改密码 / 其它会话失效
python3 scripts/regress_cache.py    # 14 项：默认后端、读路径命中、写路径失效、权限边界
python3 scripts/regress_tx.py       # 23 项：回滚真的发生、多语句写「成功三处都在 / 失败一处不留」
```

六份脚本都会在开头或结尾调用 `reset-demo`，可以任意顺序重复执行——
唯一的例外是「被踢掉的会话数」这类断言不写死条数：令牌存在进程内存里，跨多次运行会累积。

另有两份不需要启动服务的静态检查：

```bash
python3 scripts/check_layers.py     # 89 个类的跨层依赖，按白名单校验
python3 scripts/check_tx.py         # 9 个 Service：多语句写必须包在事务里、事务体不许混用注入 Mapper
```

流水线顺序建议照这个来：两项静态检查 → 装框架 → 建前端 → 编译 → 起服务 → 打接口，
失败时打印服务日志。静态检查放在最前面，因为它不需要 JDK 和编译产物，
违反依赖方向或事务约束时几秒内就失败。

> 仓库不在 `.github/workflows/` 下放这套流水线。本仓库的代码从 Gitee 同步到 GitHub，
> 推送含 workflow 文件变更的提交会被 GitHub 以权限不足拒掉，整个同步随之失败。
> 把上面几条命令接到自己的流水线即可，脚本本身是自包含的。

## 结构

按层分包，不按功能模块分包。理由：横切能力（权限校验、数据范围、审计）要加一次就覆盖全层，
按功能分会变成「改七个目录」；而 AI 定位代码靠搜索而不是浏览目录，功能内聚对人的导航价值
在这里兑现不了。

```text
src/main/java/tech/smartboot/feat/demo/plus/
├── Bootstrap.java        入口，挂载 SPA 回退
├── controller/          11 个 Controller：收参数、验权限点、调 Service
├── service/              9 个业务服务：业务规则与校验在这里，不允许直接拼 SQL
├── mapper/               8 个 MyBatis 接口 + SqlFragments（SQL 片段常量）
├── model/               11 个表结构映射，含 passwordHash / isDeleted 等不外露字段
├── dto/                 21 个请求与响应契约，OpenAPI 从这里导出
├── security/             认证与授权的机制层：AccessContext / AccessGuard /
│                         DataScopeResolver / Permission / DataScope / DepartmentTree /
│                         DepartmentSnapshot（部门全表读缓存）
├── cache/                缓存后端：CacheStore 契约 + 内存 / Redis 两实现 + 按配置选择
├── tx/                   事务模板：写操作的唯一入口（TxTemplate / TxWork / TxAction）
├── token/                令牌与会话（内存实现，可替换成 Redis）
├── common/               统一响应、异常、ApiController、SPA 回退
└── seed/                 演示数据，接入真实项目时整包删除

scripts/                  回归断言（crud / scope / audit / profile / cache / tx）
                          + 静态检查（check_layers / check_tx）
web/src/
├── services/admin-service.ts   前后端共同遵守的接口契约
├── services/http-admin-service.ts  唯一实现，页面只从这里取 adminService
├── auth/session.tsx            把当前登录人下发给页面（useSession）
├── lib/permissions.ts          权限点字典 + hasPermission
├── lib/departments.ts          部门树拍平、层级判断
└── pages/                      登录 / 概览 / 用户 / 角色 / 部门 / 审计日志 / 个人中心
```

### 依赖方向

`scripts/check_layers.py` 按白名单检查，违反即失败（CI 第一步就运行它）。

| 层 | 允许依赖 |
|---|---|
| `model` | 无（纯表结构，连 security 都不依赖） |
| `mapper` | `model` |
| `cache` | 无（只有 redisun） |
| `tx` | 无（只有 mybatis） |
| `security` | `model` `mapper` `token` `common` `cache` |
| `dto` | `model` `security` `common` |
| `service` | `dto` `model` `mapper` `security` `token` `common` `cache` `tx` |
| `controller` | `dto` `service` `security` `token` `common` `cache` `seed` |
| `common` `token` | 无 |
| `seed` | 不限制（演示设施，接入时整包删） |

两条硬规则的由来：

- **`controller` 不许直接用 `mapper`**：绕过 Service 就等于绕过数据范围校验，而这种写法
  编译能过、跑起来只是多看到几条数据，不会报错。
- **`security` 不许依赖 `service` / `controller`**：机制层一旦反向依赖业务层，两层就成环，
  之后谁也抽不走。部门子树的算法为此放在 `security/DepartmentTree`（纯函数，入参是部门行），
  `service` 与 `security` 共用同一份实现，不必各写一遍。

> **`model` 与 `dto` 是两层，不合并。** `UserAccount` 有 `passwordHash`、`isDeleted`，
> `UserItemResponse` 没有。合并成一个「实体」后，密码哈希会直接进 JSON，
> OpenAPI 导出的 schema 也会跟着错。

> **页面只能从 `@/services/http-admin-service` 导入 `adminService`。**
> 之前保留过一份 `mock-admin-service`，它的 `login()` 返回写死的 `feat-plus-demo-token`，
> 后端从未发过这个令牌，登录页误引用它时表现为「登录成功但后续每个接口都 401」。
> 现已整层删除，只留一份真实实现——契约变了 TS 会直接报错，不会静默漂移。

## 事务

没有 `@Transactional`。Feat 的装配在编译期完成，为一个注解在注解处理器里实现「传播行为、只读、
回滚规则、内层回滚外层不复位」等于把 Spring 最重的那部分搬过来，和「生成的代码人能读懂」相反。

写操作的唯一入口是 `tx/TxTemplate`：

```java
txTemplate.run(session -> {
    UserMapper users = session.getMapper(UserMapper.class);
    users.insert(user);
    users.insertUserRole(user.getId(), "ADMIN", operatorId);
});
```

边界显式、可读、可 grep；业务代码不碰 `close()`，也不需要自己 try/catch 回滚。

**为什么必须有它。**建一个用户要写三张表（`sys_user`、`sys_user_role`、`sys_activity`）。
三次自动提交意味着中途任何一步失败都会留下半截数据——「有账号没角色」的账号登不进去但列表里有，
「有账号没日志」的变更查不到是谁做的——而接口返回的是 200，没有人会知道。

三条规则，都是「做错了不报错」的类型：

- **事务体里只能用 `session.getMapper(...)`。** 注入进来的 Mapper 是生成代码包过的版本，
  每个方法自己 `openSession(true)` 独立提交。混用一个，那一笔写就落在事务外。
- **跨 Service 调用方也走模板时才传播。** 内层 `txTemplate` 会加入外层会话（线程绑定），
  同生共死；对方若直接用手上的注入 Mapper 写，仍然是独立提交——所以上一条对每一层都成立。
- **缓存失效放在事务之外。** 提交前清缓存，别的线程会立刻用提交前的数据把缓存填回去，
  「改完已生效」就变成「改完十分钟后才生效」，而且只在有并发时出现。

审计记录 `ActivityRecorder` 也走模板，因此**和被审计的变更同生共死**：回滚时日志一起消失，
不会出现「日志说建了、账号却不在」。失败的操作不留痕是对的——审计记的是「谁改了什么」，
没改成的操作属于访问日志的范畴。

`scripts/check_tx.py` 把上面这些东西变成可验证的约束：一个方法里对注入 Mapper 写两次以上
必须包模板、模板体内不许出现注入 Mapper、方法里有模板时写必须全在模板内。**跑一次就知道有没有
人绕过去了**——这类问题的特点是人能记住、写代码的那个东西记不住。

部署到新环境后可以打一次 `GET /api/system/selftest`（要求 `system:write`）：它在一个事务里写一行、
故意抛异常，再在事务外回查，确认提交真的没发生。换库、换驱动、上容器之后先跑这一下，
比等某次上线少提交一笔便宜得多。

## 缓存

默认进程内（`MemoryCacheStore`），把 `feat.yaml` 的 `feat-plus.cache.provider` 改成 `redis` 即切换后端，
业务代码不动。目前接在「部门全表读」上（`security/DepartmentSnapshot`）：数据范围解析每请求都要
整表算一次子树，而部门数据一个月改不了几次。

```yaml
feat-plus:
  cache:
    provider: memory        # memory | redis
    default-ttl-seconds: 300
    key-prefix: "plus:cache:"
    redis:
      address: 127.0.0.1:6379
```

三条规则，都是踩过之后写死的：

- **TTL 和主动失效同时存在**，不是二选一。TTL 兜住漏失效的路径，主动失效保证改完立刻生效。
- **写方法内部的读不走缓存**。否则看不到自己刚插入的行，或者更糟——拿已删除的部门当有效数据，
  让数据权限的范围悄悄放宽。
- **配置写错就启动失败**，不静默降级到内存。缓存降级后应用照常工作、只是慢，没人会发现配置
  根本没生效；多实例下它的表现是「偶发读到旧数据」，比启动失败难查得多。

`feat.yaml` 是编译期输入，改完要 `mvn clean compile`。只跑 `mvn compile` 时 Maven 可能判定为增量构建，
注解处理器不重新执行，跑起来还是上一套配置。

运维侧有一个只读接口：`GET /api/system/cache`（要求 `system:write`），返回当前后端与本次启动以来的
命中计数。它的用途是回答「现在到底在用哪个后端」——这个信息无法从部署包推断。

## 换成真实数据库

`src/main/resources/mybatis/mybatis-config.xml` 里改掉 H2 的 `jdbc:h2:mem:...` 即可。
表结构在 `mybatis/ddl/schema.sql`，`initial-sql` 会在启动时执行。

## 这个 demo 暴露的几个 Feat 限制

写的过程中撞到的，都在代码注释里标了原因：

1. **`@Mapper` 接口里不能有常量字段，也不能有内部类。** 注解处理器会把接口内所有元素当作方法强转 `ExecutableElement`，字段是 `VarSymbol`，直接抛 `cannot be cast to ExecutableElement`。SQL 片段已挪到 `mapper/SqlFragments`。
2. **`@Value` 需要 setter 方法**，字段注入会编译失败：`no setter method for field[...]`。
3. **`@Autowired` 按字段名匹配 Bean。** `TokenStore` 的实现类默认注册名是 `inMemoryTokenStore`，必须写成 `@Bean("tokenStore")` 才能与字段名对上。同理 `TxTemplate` 的注入字段必须叫 `txTemplate`——写成 `tx` 不会退化成空值，而是启动时抛 `bean[tx] not ready`。
4. **`HttpStaticResourceHandler` 的同步 `handle(HttpRequest)` 是空实现**，直接抛 `UnsupportedOperationException`，只能在异步链路里调用。
5. **SPA 路由没有内置回退。** 默认静态处理器找不到 `/users` 会去试 `users.html`，因此刷新前端路由会 404，需要自己挂 `SpaFallbackHandler`。
6. **没有全局异常处理。** 401/404 这类状态码要在 Controller 里显式 `setHttpStatus`，见 `common/ApiController`。
7. **返回值序列化在编译期完成**，逐字段调 getter，没有对应字段的计算属性会被整条丢掉。
   `CacheStats` 的 `hitRate` 原本只在 getter 里算，接口返回里就少了这个键——必须落成真实字段。
8. **启动异常的中间层 message 会消失**。`new FeatException(msg, e)` 会把 cause 解包到最底层，
   所以 `new IllegalStateException(msg, e)` 里的提示打不出来，控制台只剩 `Connection refused`。
   要让提示可见，把信息拼进自己的 message 并且不设 cause。
