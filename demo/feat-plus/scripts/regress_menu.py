#!/usr/bin/env python3
"""菜单与权限字典的回归用例。

菜单从「前端三份硬编码」改成「后端一处定义、随 /me 下发」之后，能出错的地方变了：

1. **菜单跟着权限过滤，而且是服务端过滤**：只返回这个人能看的，不是前端拿到全量再藏起来。
   前端藏起来的东西，改个 localStorage 就能看见；
2. **菜单与权限字典同源**：`/api/permissions` 的 key 集合必须等于 ADMIN 的全量权限点，
   两份一旦不一致，表现是「角色页里没有这个勾选项，接口却在校验它」；
3. **顺序稳定**：菜单顺序由注册表的 sort 决定，不依赖 HashMap 遍历顺序。
   顺序抖动不会报错，只会让 AI 与人看到的界面每次都不一样。

运行前需先启动服务（默认 8080）。脚本会重置演示数据。
"""
import json
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
check("菜单条数", 6, len(me_menus))
check("首个菜单是数据概览", "/dashboard", paths({"menus": me_menus})[0])
check("分组顺序", ["workspace", "system", "account"],
      list(dict.fromkeys([item.get("groupKey") for item in me_menus])))
check("个人中心不进侧栏", False, [item.get("visible") for item in me_menus if item.get("path") == "/profile"][0])
check_true("个人中心仍然有标题",
           [item.get("name") for item in me_menus if item.get("path") == "/profile"][0] == "个人中心")
check_true("每个菜单项都带图标 key",
           all(item.get("icon") for item in me_menus),
           [item.get("icon") for item in me_menus])

admin_permissions = set(me(admin).get("permissions", []))
check_true("每个菜单项都带 permission 键（不要用 null，两个接口的字段集合会不一致）",
           all("permission" in item for item in me_menus))
check_true("菜单声明的权限点都在权限列表里",
           all(item.get("permission") in admin_permissions for item in me_menus if item.get("permission")),
           "%d 个菜单" % len(me_menus))

print("--- 权限点字典由后端下发 ---")
status, body = call("GET", "/api/permissions", admin)
catalog = body.get("data", [])
check("字典接口状态", 200, status)
check("字典条数", 9, len(catalog))
check("字典 key 集合与管理员权限一致", sorted(admin_permissions), sorted(item.get("key") for item in catalog))
check("字典分组", ["dashboard", "user", "role", "dept", "audit", "system"],
      list(dict.fromkeys([item.get("groupKey") for item in catalog])))
check_true("每条都有中文名与说明",
           all(item.get("label") and item.get("description") for item in catalog))
check_true("字典顺序稳定（两次请求一致）",
           [item.get("key") for item in catalog] == [item.get("key") for item in call("GET", "/api/permissions", admin)[1].get("data", [])])

print("--- 按角色过滤：服务端返回的就是能看的 ---")
bob = login("bob")
bob_user = me(bob)
check("普通成员只有数据概览与个人中心", ["/dashboard", "/profile"], paths(bob_user))
check("普通成员看不到成员管理菜单", False, "/users" in paths(bob_user))

alice = login("alice")
check("部门主管（USER + DEPT_MANAGER）多看一个成员管理", ["/dashboard", "/users", "/profile"], paths(me(alice)))

carol = me(login("carol"))
check("审计员看得到五个管理菜单",
      ["/dashboard", "/users", "/roles", "/departments", "/activities", "/profile"], paths(carol))

# 用「管理员的全量注册表 + 各自权限」推导期望值，验证过滤逻辑本身，而不是把结果写死。
for name, token in (("bob", bob), ("alice", alice)):
    user = me(token)
    owned = set(user.get("permissions", []))
    expected = [item.get("path") for item in me_menus
                if not item.get("permission") or item.get("permission") in owned]
    check("%s 的菜单 = 按权限过滤注册表" % name, expected, paths(user))

print("--- 边界 ---")
status, body = call("GET", "/api/permissions", bob)
check("没有 role:read 的人拿不到字典", 403, status)
status, _ = call("GET", "/api/permissions")
check("未登录拿不到字典", 401, status)
status, _ = call("GET", "/api/auth/me")
check("未登录拿不到菜单", 401, status)

print()
print("通过 %d / 失败 %d" % (passed, failed))
raise SystemExit(1 if failed else 0)
