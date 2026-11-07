#!/usr/bin/env python3
"""菜单与权限字典的回归用例。

菜单从「代码里的注册表」改成「数据库里的配置」之后，能出错的地方全变了，这个脚本按新的错法来写：

1. **路径必须是白名单里的页面**。这是入库之后新增的风险：填错一个字符串不会报错，
   表现是「侧栏有这一条，点进去被弹回首页」。所以非法路径必须被 400 拦住；
2. **同一页面只能有一个入口**。允许重复的话，侧栏会出现两个都指向同一页面的项；
3. **改完必须立刻生效**。菜单有 10 分钟缓存，靠写操作后的主动失效保证；漏了失效的表现是
   「改完标题刷新还是旧的」，所以每条写操作后都回查 /me，而不是只查管理接口；
4. **菜单按权限过滤，而且是服务端过滤**。前端藏起来的东西，改 localStorage 就能看见；
5. **无权页面随 /me 点名（restrictedPaths）**。侧栏过滤只藏入口，直达仍能打开页面；
   前端据此在进入前拦住。它必须与侧栏过滤同源同权，否则两边各说各话；
6. **权限点字典仍由代码定义**。它是「接口校验了什么」的镜像，不是可运营的数据；
   每个权限点带 menuCount，说明它控制着几个菜单入口。

运行前需先启动服务（默认 8080）。脚本会重置演示数据，并在结尾再重置一次。
"""
import json
import urllib.error
import urllib.request

BASE = "http://127.0.0.1:8080"
# 沙箱设了 HTTP_PROXY，访问本机必须显式绕过，否则会拿到代理返回的 502。
OPENER = urllib.request.build_opener(urllib.request.ProxyHandler({}))

passed = 0
failed = 0


def call(method, path, token=None, body=None):
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(BASE + path, data=data, method=method)
    req.add_header("Accept", "application/json")
    if data is not None:
        req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    try:
        with OPENER.open(req) as resp:
            return resp.status, json.loads(resp.read().decode() or "{}")
    except urllib.error.HTTPError as e:
        raw = e.read().decode()
        try:
            return e.code, json.loads(raw or "{}")
        except json.JSONDecodeError:
            return e.code, {"message": raw[:120]}


def login(username, password="admin123"):
    status, body = call("POST", "/api/auth/login", body={"username": username, "password": password})
    return body.get("data", {}).get("accessToken") if status == 200 else None


def me(token):
    status, body = call("GET", "/api/auth/me", token)
    return body.get("data", {}) if status == 200 else {}


def paths(user):
    return [item.get("path") for item in user.get("menus", [])]


def admin_menus(token):
    """管理接口的全量列表，path -> 行。"""
    status, body = call("GET", "/api/menus", token)
    assert status == 200, "读取菜单列表失败：%s" % body
    return {item.get("path"): item for item in body.get("data", [])}


def check(name, expected, actual):
    global passed, failed
    if expected == actual:
        passed += 1
        print("  OK   %s -> %s" % (name, actual))
    else:
        failed += 1
        print("  FAIL %s  期望 %s 实际 %s" % (name, expected, actual))


def check_true(name, condition, detail=""):
    global passed, failed
    if condition:
        passed += 1
        print("  OK   %s %s" % (name, detail))
    else:
        failed += 1
        print("  FAIL %s %s" % (name, detail))


admin = login("admin")
if not admin:
    raise SystemExit("登录失败：请先启动服务")
call("POST", "/api/admin/reset-demo", admin)
admin = login("admin")

print("--- 管理员：全量菜单 ---")
status, body = call("POST", "/api/auth/login", body={"username": "admin", "password": "admin123"})
login_menus = body.get("data", {}).get("user", {}).get("menus", [])
me_menus = me(admin).get("menus", [])
# 逐字段比对而不是只比路径：这两个接口的嵌套深度不同（login 是 RestResult→LoginResponse→User→Menu，
# 比 /me 多一层），AOT 生成器在深度超限时会退化成 fastjson2 序列化，字段集合可能因此漂移。
check("登录响应与 /me 的菜单逐字段一致", me_menus, login_menus)
check("登录响应与 /me 的 restrictedPaths 一致",
      me(admin).get("restrictedPaths"), body.get("data", {}).get("user", {}).get("restrictedPaths"))
check("菜单条数", 7, len(me_menus))
check("首个菜单是数据概览", "/dashboard", paths({"menus": me_menus})[0])
check("分组顺序", ["workspace", "system", "account"],
      list(dict.fromkeys([item.get("groupKey") for item in me_menus])))
check("个人中心不进侧栏", False, [item.get("visible") for item in me_menus if item.get("path") == "/profile"][0])
check_true("个人中心仍然有标题",
           [item.get("name") for item in me_menus if item.get("path") == "/profile"][0] == "个人中心")
check_true("每个菜单项都带图标 key", all(item.get("icon") for item in me_menus),
           [item.get("icon") for item in me_menus])
check_true("每个菜单项都带 permission 键（不要用 null，两个接口的字段集合会不一致）",
           all("permission" in item for item in me_menus))

admin_permissions = set(me(admin).get("permissions", []))
check_true("菜单声明的权限点都在权限列表里",
           all(item.get("permission") in admin_permissions for item in me_menus if item.get("permission")),
           "%d 个菜单" % len(me_menus))

print("--- 权限点字典仍由代码定义 ---")
status, body = call("GET", "/api/permissions", admin)
catalog = body.get("data", [])
check("字典接口状态", 200, status)
check("字典条数", 11, len(catalog))
check("字典 key 集合与管理员权限一致", sorted(admin_permissions), sorted(item.get("key") for item in catalog))
counts = {item.get("key"): item.get("menuCount") for item in catalog}
check("dashboard:read 挂在 1 条菜单上", 1, counts.get("dashboard:read"))
check("audit:read 挂在 1 条菜单上", 1, counts.get("audit:read"))
check("menu:write 只管接口、不控制菜单入口", 0, counts.get("menu:write"))
check("system:write 只管接口、不控制菜单入口", 0, counts.get("system:write"))
check_true("每条都带 menuCount 键（int，不要缺字段）", all("menuCount" in item for item in catalog))
check("字典分组", ["dashboard", "user", "role", "dept", "audit", "system"],
      list(dict.fromkeys([item.get("groupKey") for item in catalog])))
check_true("每条都有中文名与说明", all(item.get("label") and item.get("description") for item in catalog))
check_true("字典顺序稳定（两次请求一致）",
           [item.get("key") for item in catalog]
           == [item.get("key") for item in call("GET", "/api/permissions", admin)[1].get("data", [])])

print("--- 按角色过滤：服务端返回的就是能看的 ---")
bob = login("bob")
bob_user = me(bob)
check("普通成员只有数据概览与个人中心", ["/dashboard", "/profile"], paths(bob_user))
alice = login("alice")
check("部门主管（USER + DEPT_MANAGER）多看一个成员管理", ["/dashboard", "/users", "/profile"], paths(me(alice)))
carol_token = login("carol")
carol = me(carol_token)
check("审计员看得到六个管理菜单",
      ["/dashboard", "/users", "/roles", "/departments", "/activities", "/profile"], paths(carol))
for name, token in (("bob", bob), ("alice", alice)):
    user = me(token)
    owned = set(user.get("permissions", []))
    expected = [item.get("path") for item in me_menus
                if not item.get("permission") or item.get("permission") in owned]
    check("%s 的菜单 = 按权限过滤全量菜单" % name, expected, paths(user))

print("--- 直达拦截：无权页面随 /me 点名 ---")
# 侧栏过滤只藏入口：知道地址的人仍能在地址栏打开 /users，然后面对一屏 403。
# restrictedPaths 把「配置了菜单且当前人无权」的页面点名下发，前端据此在进入前拦住并说明原因。
# 它必须与 visibleFor 读同一份数据、同一次权限合并，否则侧栏和直达拦截会各说各话。
check("普通成员的 restrictedPaths", ["/users", "/roles", "/departments", "/activities", "/menus"],
      bob_user.get("restrictedPaths"))
check("部门主管少了 /users", ["/roles", "/departments", "/activities", "/menus"],
      me(alice).get("restrictedPaths"))
check("审计员只被拦菜单管理", ["/menus"], me(carol_token).get("restrictedPaths"))
check("管理员无拦截（空列表，不是 null）", [], me(admin).get("restrictedPaths"))

print("--- 管理接口的权限边界 ---")
check("没有 menu:read 的人读不到菜单配置", 403, call("GET", "/api/menus", bob)[0])
check("没有 menu:write 的人改不了菜单", 403, call("POST", "/api/menus", bob, {
    "groupKey": "workspace", "groupLabel": "工作台", "path": "/dashboard", "name": "x"})[0])
check("未登录读不到菜单配置", 401, call("GET", "/api/menus")[0])
check("没有 menu:write 的人拿不到路径白名单", 403, call("GET", "/api/menus/routes", carol_token)[0])

print("--- 路径白名单：菜单只能指向已经存在的页面 ---")
status, body = call("GET", "/api/menus/routes", admin)
routes = body.get("data", [])
check("白名单条数", 7, len(routes))
check_true("白名单含菜单管理自己", "/menus" in routes, routes)

base = {"groupKey": "system", "groupLabel": "系统管理", "groupSort": 20, "icon": "scroll",
        "permission": "audit:read", "sort": 30, "visible": True}
status, body = call("POST", "/api/menus", admin, dict(base, path="/orders", name="订单管理"))
check("路径不在白名单时拒绝", 400, status)
check_true("提示里说明可选路径", "页面不存在" in (body.get("message") or ""), body.get("message"))

status, body = call("POST", "/api/menus", admin, dict(base, path="/users", name="成员（重复）"))
check("同一页面重复建菜单时拒绝", 400, status)
check_true("提示里说明一个页面一个入口", "已经有菜单" in (body.get("message") or ""), body.get("message"))

status, body = call("POST", "/api/menus", admin, dict(base, path="/dashboard", name="概览（重复）"))
check("第二个重复页面也被拒绝", 400, status)

status, body = call("POST", "/api/menus", admin,
                    dict(base, path="/roles", name="角色（坏权限点）", permission="nope:read"))
check("未知权限点时拒绝", 400, status)
check_true("提示里带上未知的 key", "nope:read" in (body.get("message") or ""), body.get("message"))

status, body = call("POST", "/api/menus", admin, dict(base, path="/roles", name="   ", permission=""))
check("菜单名称为空白时拒绝", 400, status)

print("--- 增删改：改完立刻生效（缓存失效） ---")
before = admin_menus(admin)
activities = before["/activities"]
status, _ = call("DELETE", "/api/menus/%d" % activities["id"], admin)
check("删除审计日志菜单", 200, status)
check("/me 里立刻消失", 6, len(me(admin).get("menus", [])))
check("审计日志确实不在菜单里", False, "/activities" in paths(me(admin)))

status, body = call("POST", "/api/menus", admin, {
    "groupKey": "system", "groupLabel": "系统管理", "groupSort": 20, "path": "/activities",
    "name": "操作记录", "icon": "scroll", "permission": "audit:read", "sort": 35, "visible": True})
check("重新创建同一路径（软删后可以复用）", 200, status)
created = body.get("data", {})
check("新菜单名", "操作记录", created.get("name"))
check("/me 里立刻出现新名字", "操作记录",
      [item.get("name") for item in me(admin).get("menus", []) if item.get("path") == "/activities"][0])
check("普通成员仍然看不到（需要 audit:read）", False, "/activities" in paths(me(bob)))
check("审计员看得到（有 audit:read）", True, "/activities" in paths(me(carol_token)))

status, _ = call("PUT", "/api/menus/%d" % created["id"], admin, {
    "groupKey": "system", "groupLabel": "系统管理", "groupSort": 20, "path": "/activities",
    "name": "审计轨迹", "icon": "scroll", "permission": "", "sort": 35, "visible": False})
check("改名为审计轨迹", 200, status)
menu_after = me(admin).get("menus", [])
check("/me 里立刻是新名字", "审计轨迹",
      [item.get("name") for item in menu_after if item.get("path") == "/activities"][0])
check("改成登录可见后普通成员也能看到", True, "/activities" in paths(me(bob)))
check("权限点清空后 restrictedPaths 不再点名 /activities",
      False, "/activities" in me(bob).get("restrictedPaths", []))
check("隐藏后不进侧栏但仍是标题来源", False,
      [item.get("visible") for item in menu_after if item.get("path") == "/activities"][0])

status, _ = call("PUT", "/api/menus/%d" % created["id"], admin, {
    "groupKey": "workspace", "groupLabel": "工作台", "groupSort": 10, "path": "/activities",
    "name": "审计轨迹", "icon": "scroll", "permission": "", "sort": 99, "visible": True})
check("把菜单挪到别的分组", 200, status)
moved = [item for item in me(admin).get("menus", []) if item.get("path") == "/activities"][0]
check("分组跟着变", "workspace", moved.get("groupKey"))
check("组名跟着变", "工作台", moved.get("groupLabel"))

print("--- 分组名是整组的事 ---")
status, _ = call("PUT", "/api/menus/%d" % created["id"], admin, {
    "groupKey": "workspace", "groupLabel": "常用", "groupSort": 5, "path": "/activities",
    "name": "审计轨迹", "icon": "scroll", "permission": "", "sort": 99, "visible": True})
check("改组名", 200, status)
workspace_labels = {item.get("groupLabel") for item in me(admin).get("menus", [])
                    if item.get("groupKey") == "workspace"}
check("同组其它行跟着改名（不是只改一行）", {"常用"}, workspace_labels)

print("--- 删除的影响面 ---")
status, _ = call("DELETE", "/api/menus/%d" % created["id"], admin)
check("删除测试菜单", 200, status)
check("侧栏少一条", 6, len(me(admin).get("menus", [])))
# 菜单删掉后就不是「配置了但无权」了，从 restrictedPaths 里消失——它拦的是配置，不是页面本身。
check("删除后 restrictedPaths 也不再点名该页面",
      ["/users", "/roles", "/departments", "/menus"], me(bob).get("restrictedPaths"))
check("删不存在的菜单返回 404", 404, call("DELETE", "/api/menus/%d" % created["id"], admin)[0])
check("改不存在的菜单返回 404", 404, call("PUT", "/api/menus/999999", admin, {
    "groupKey": "system", "groupLabel": "系统管理", "path": "/roles", "name": "x"})[0])

print("--- 菜单变更进审计 ---")
status, body = call("GET", "/api/activities?page=1&pageSize=20", admin)
titles = [item.get("title") for item in body.get("data", {}).get("items", [])]
check_true("审计里留下了菜单改动", any("菜单" in (title or "") for title in titles), titles[:3])

print("--- 边界 ---")
status, _ = call("GET", "/api/auth/me")
check("未登录拿不到菜单", 401, status)

# 恢复现场：上面的用例删改过菜单，重置回种子状态，后面的脚本才能按固定数据断言。
call("POST", "/api/admin/reset-demo", admin)

print()
print("通过 %d / 失败 %d" % (passed, failed))
raise SystemExit(1 if failed else 0)
