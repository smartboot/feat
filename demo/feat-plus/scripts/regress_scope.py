#!/usr/bin/env python3
"""多角色与数据权限的回归用例。

覆盖四件事：
1. 多角色的权限并集与数据范围取最宽；
2. 列表过滤 + 按 id 的越权闸门（只做前者等于没做）；
3. 角色数据范围配置的往返（自定义部门反复编辑不能撞主键）；
4. 审计日志与首页近期动态同样受数据范围约束（按操作人所属部门过滤）。

运行前需先启动服务（默认 8080），脚本开头会重置演示数据。
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


def check(name, expected, actual):
    global passed, failed
    if expected == actual:
        passed += 1
        print("  OK   %s -> %s" % (name, actual))
    else:
        failed += 1
        print("  FAIL %s  期望 %s 实际 %s" % (name, expected, actual))


def me(token):
    return call("GET", "/api/auth/me", token)[1]["data"]


admin = login("admin")
call("POST", "/api/admin/reset-demo", admin)
admin = login("admin")
alice = login("alice")
bob = login("bob")
carol = login("carol")

# 部门与角色的 id 从接口取，避免硬编码顺序
_, tree = call("GET", "/api/departments", admin)
DEPTS = {}


def walk(nodes):
    for node in nodes:
        DEPTS[node["code"]] = node["id"]
        walk(node["children"])


walk(tree["data"])
ROLES = {r["code"]: r["id"] for r in call("GET", "/api/roles", admin)[1]["data"]}

PMC_DESIGN = DEPTS["PMC_DESIGN"]   # alice 所在部门，无下级
CS = DEPTS["CS"]                   # bob 所在部门，无下级
FIN = DEPTS["FIN"]
TECH = DEPTS["TECH"]

print("--- 身份：多角色并集 ---")
a = me(admin)
check("admin 角色", ["ADMIN"], a["roleCodes"])
check("admin 数据范围", 1, a["dataScope"])
check("admin 权限数（全部 11 项）", 11, len(a["permissions"]))

al = me(alice)
check("alice 挂两个角色", ["DEPT_MANAGER", "USER"], sorted(al["roleCodes"]))
check("alice 数据范围取最宽（本部门及以下）", 4, al["dataScope"])
check("alice 权限是两角色并集", ["dashboard:read", "user:read"], sorted(al["permissions"]))
check("alice 范围说明", "本部门及以下（1 个部门）", al["scopeDescription"])

b = me(bob)
check("bob 只有 dashboard:read", ["dashboard:read"], b["permissions"])
check("bob 数据范围=仅本人", 5, b["dataScope"])

print("--- 数据范围：列表过滤 ---")
status, body = call("GET", "/api/users?page=1&pageSize=50", admin)
check("admin 看到 12 人", 12, body["data"]["total"])
status, body = call("GET", "/api/users?page=1&pageSize=50", alice)
check("alice 只看到产品设计部 2 人", 2, body["data"]["total"])
check("alice 看到的是自己和 iris", ["alice", "iris"], sorted(i["username"] for i in body["data"]["items"]))
_, body = call("GET", "/api/users?page=1&pageSize=50", carol)
check("carol(AUDITOR=全部) 看到 12 人", 12, body["data"]["total"])
status, _ = call("GET", "/api/users", bob)
check("bob 无 user:read -> 403", 403, status)
check("alice 下拉列表也只有 2 人", 2, len(call("GET", "/api/users/all", alice)[1]["data"]))
check("carol 下拉列表 12 人", 12, len(call("GET", "/api/users/all", carol)[1]["data"]))

print("--- 数据范围：按 id 越权闸门 ---")
alice_id = [i for i in call("GET", "/api/users", alice)[1]["data"]["items"] if i["username"] == "alice"][0]["id"]
iris_id = [i for i in call("GET", "/api/users", alice)[1]["data"]["items"] if i["username"] == "iris"][0]["id"]
check("alice 读自己 200", 200, call("GET", "/api/users/%d" % alice_id, alice)[0])
check("alice 读同部门 200", 200, call("GET", "/api/users/%d" % iris_id, alice)[0])
check("alice 读范围外 -> 404（不暴露存在性）", 404, call("GET", "/api/users/1", alice)[0])
check("alice 改范围外 -> 403", 403, call("PUT", "/api/users/1", alice, {
    "username": "admin", "displayName": "x", "roleCodes": ["ADMIN"],
    "departmentId": TECH, "email": "admin@feat.plus", "status": 1})[0])
check("alice 删范围外 -> 403", 403, call("DELETE", "/api/users/3", alice)[0])
check("alice 重置范围外密码 -> 403", 403, call("POST", "/api/users/1/reset-password", alice)[0])
status, body = call("PUT", "/api/users/1", alice, {
    "username": "admin", "displayName": "x", "roleCodes": ["ADMIN"],
    "departmentId": TECH, "email": "admin@feat.plus", "status": 1})
check("越权写入被拒绝", 403, status)

print("--- 数据范围：首页统计同口径 ---")
check("alice 首页 totalUsers=2", 2, call("GET", "/api/dashboard", alice)[1]["data"]["totalUsers"])
check("admin 首页 totalUsers=12", 12, call("GET", "/api/dashboard", admin)[1]["data"]["totalUsers"])

print("--- 角色：内置数据范围 ---")
roles = {r["code"]: r for r in call("GET", "/api/roles", admin)[1]["data"]}
check("内置角色 4 个", 4, len(roles))
check("ADMIN=全部", 1, roles["ADMIN"]["departmentScope"])
check("DEPT_MANAGER=本部门及以下", 4, roles["DEPT_MANAGER"]["departmentScope"])
check("USER=仅本人", 5, roles["USER"]["departmentScope"])
check("ADMIN 权限固定 11 项", 11, len(roles["ADMIN"]["permissions"]))

print("--- 角色：自定义部门往返 ---")
status, created = call("POST", "/api/roles", admin, {
    "code": "SCOPED", "name": "受控角色", "description": "回归用",
    "permissions": ["user:read"], "departmentScope": 2,
    "departmentIds": [PMC_DESIGN, FIN], "status": 1})
check("创建自定义范围角色", 200, status)
rid = created["data"]["id"]
check("回显两个部门", sorted([PMC_DESIGN, FIN]), sorted(created["data"]["departmentIds"]))
check("范围名称", "自定义部门", created["data"]["scopeLabel"])

status, updated = call("PUT", "/api/roles/%d" % rid, admin, {
    "name": "受控角色", "description": "回归用", "permissions": ["user:read"],
    "departmentScope": 3, "status": 1})
check("改为本部门", 200, status)
check("非自定义档位不残留部门", [], updated["data"]["departmentIds"])

status, updated = call("PUT", "/api/roles/%d" % rid, admin, {
    "name": "受控角色", "description": "回归用", "permissions": ["user:read"],
    "departmentScope": 2, "departmentIds": [PMC_DESIGN], "status": 1})
check("改回自定义（软删后重新启用）", 200, status)
check("部门回到 1 个", [PMC_DESIGN], updated["data"]["departmentIds"])

status, updated = call("PUT", "/api/roles/%d" % rid, admin, {
    "name": "受控角色", "description": "回归用", "permissions": ["user:read"],
    "departmentScope": 2, "departmentIds": [PMC_DESIGN], "status": 1})
check("重复提交同一范围不撞主键", 200, status)

print("--- 角色：内置保护 ---")
status, body = call("PUT", "/api/roles/%d" % ROLES["ADMIN"], admin, {
    "name": "系统管理员", "description": "x", "permissions": ["user:read"],
    "departmentScope": 1, "status": 1})
check("ADMIN 权限不可改 -> 400", 400, status)
status, body = call("PUT", "/api/roles/%d" % ROLES["ADMIN"], admin, {
    "name": "系统管理员", "description": "x", "permissions": me(admin)["permissions"],
    "departmentScope": 5, "status": 1})
check("ADMIN 数据范围不可改 -> 400", 400, status)
status, _ = call("DELETE", "/api/roles/%d" % ROLES["DEPT_MANAGER"], admin)
check("有成员在用的内置角色不可删 -> 400", 400, status)

print("--- 多角色：分配与回收 ---")
status, body = call("PUT", "/api/users/3", admin, {
    "username": "bob", "displayName": "周明远", "roleCodes": ["USER", "SCOPED"],
    "departmentId": CS, "email": "bob@feat.plus", "status": 1})
check("给 bob 加 SCOPED", 200, status)
check("bob 现在两个角色", ["SCOPED", "USER"], sorted(body["data"]["roleCodes"]) if "data" in body else None)
bob = login("bob")
status, body = call("GET", "/api/users", bob)
check("bob 因 SCOPED 获得 user:read", 200, status)
check("bob 看到产品设计部 2 人", 2, body["data"]["total"])
check("bob 范围=自定义", "自定义部门（1 个部门）", me(bob)["scopeDescription"])

status, body = call("PUT", "/api/users/3", admin, {
    "username": "bob", "displayName": "周明远", "roleCodes": ["USER"],
    "departmentId": CS, "email": "bob@feat.plus", "status": 1})
check("收回 SCOPED", 200, status)
check("角色回落到单个", ["USER"], body["data"]["roleCodes"])
bob = login("bob")
check("bob 又看不到成员了 -> 403", 403, call("GET", "/api/users", bob)[0])

status, body = call("PUT", "/api/users/3", admin, {
    "username": "bob", "displayName": "周明远", "roleCodes": ["USER", "SCOPED"],
    "departmentId": CS, "email": "bob@feat.plus", "status": 1})
check("再加回来（走恢复分支）", 200, status)
check("角色名返回中文", sorted(["普通成员", "受控角色"]), sorted(body["data"]["roleNames"]))

print("--- 创建用户：范围校验 ---")
status, _ = call("POST", "/api/users", alice, {
    "username": "outsider", "displayName": "越权", "roleCodes": ["USER"],
    "departmentId": FIN, "email": "outsider@feat.plus", "status": 1})
check("alice 无 user:write -> 403", 403, status)

status, _ = call("PUT", "/api/roles/%d" % rid, admin, {
    "name": "受控角色", "description": "回归用", "permissions": ["user:read", "user:write"],
    "departmentScope": 4, "status": 1})
check("给 SCOPED 加写权限", 200, status)
status, body = call("POST", "/api/users", bob, {
    "username": "in_scope", "displayName": "范围内", "roleCodes": ["USER"],
    "departmentId": CS, "email": "in_scope@feat.plus", "status": 1})
check("在自己部门建人 200", 200, status)
new_id = body["data"]["id"] if status == 200 else 0
status, _ = call("POST", "/api/users", bob, {
    "username": "out_scope", "displayName": "范围外", "roleCodes": ["USER"],
    "departmentId": FIN, "email": "out_scope@feat.plus", "status": 1})
check("在范围外部门建人 403", 403, status)

print("--- 管理员兜底 ---")
status, body = call("PUT", "/api/users/1", admin, {
    "username": "admin", "displayName": "系统管理员", "roleCodes": ["USER"],
    "departmentId": DEPTS["TECH_RD"], "email": "admin@feat.plus", "status": 1})
check("不能移除最后一个管理员的 ADMIN 角色", 400, status)
status, body = call("POST", "/api/users/1/status", admin, {"status": 0})
check("不能停用当前登录账号", 400, status)

print("--- 审计日志的数据范围 ---")
# 这一段单独重置一次：日志条数会被前面的用例改得面目全非，
# 而「受限角色看到几条」必须从一个确定的基线出发才有意义。
call("POST", "/api/admin/reset-demo", admin)
admin = login("admin")
_, body = call("GET", "/api/users?keyword=bob&page=1&pageSize=10", admin)
bob_id = body["data"]["items"][0]["id"]

# 先断言基线，再建角色：建角色本身也会写一条日志，顺序反了就会数错。
status, body = call("GET", "/api/activities?page=1&pageSize=20", admin)
check("admin 看到全部 4 条日志", 4, body["data"]["total"])
check("admin 的操作人下拉 3 人（系统日志无操作人）", 3,
      len(call("GET", "/api/activities/operators", admin)[1]["data"]))
check("alice 没有 audit:read", 403, call("GET", "/api/activities", alice)[0])

status, body = call("POST", "/api/roles", admin, {
    "code": "AUDIT_FIN", "name": "财务审计（受限）", "description": "只读财务部日志",
    "permissions": ["audit:read", "dashboard:read"], "departmentScope": 2,
    "departmentIds": [FIN], "status": 1})
check("建受限审计角色", 200, status)
audit_role_id = body["data"]["id"]

status, _ = call("PUT", "/api/users/%d" % bob_id, admin, {
    "username": "bob", "displayName": "周明远", "roleCodes": ["USER", "AUDIT_FIN"],
    "departmentId": CS, "email": "bob@feat.plus", "status": 1})
check("bob 挂上受限审计角色", 200, status)
bob = login("bob")

status, body = call("GET", "/api/activities?page=1&pageSize=20", bob)
check("bob 只看到财务部那条", 1, body["data"]["total"])
check("bob 看到的是财务部操作人", ["林书雅"], [i["operatorName"] for i in body["data"]["items"]])
check("列表与计数一致（不能只过滤 count）", body["data"]["total"], len(body["data"]["items"]))
check("bob 的操作人下拉同步收窄", ["林书雅"],
      [o["name"] for o in call("GET", "/api/activities/operators", bob)[1]["data"]])
check("bob 首页近期动态同步收窄", 1, len(call("GET", "/api/dashboard", bob)[1]["data"]["activities"]))

status, _ = call("PUT", "/api/roles/%d" % audit_role_id, admin, {
    "name": "财务审计（受限）", "description": "t",
    "permissions": ["audit:read", "dashboard:read"], "departmentScope": 5, "status": 1})
check("改为仅本人", 200, status)
check("仅本人时看不到别人的日志", 0, call("GET", "/api/activities", bob)[1]["data"]["total"])

status, _ = call("PUT", "/api/roles/%d" % audit_role_id, admin, {
    "name": "财务审计（受限）", "description": "t",
    "permissions": ["audit:read", "dashboard:read"], "departmentScope": 1, "status": 1})
check("改为全部数据", 200, status)
# 不写死条数：前面几步已经在写日志了。真正要断言的是「范围放开后与管理员看到的一样多」。
check("全部数据时与 admin 看到的一样多",
      call("GET", "/api/activities", admin)[1]["data"]["total"],
      call("GET", "/api/activities", bob)[1]["data"]["total"])

status, _ = call("PUT", "/api/roles/%d" % audit_role_id, admin, {
    "name": "财务审计（受限）", "description": "t",
    "permissions": ["audit:read", "dashboard:read"], "departmentScope": 2,
    "departmentIds": [FIN], "status": 1})
check("从全部切回自定义（验证 sys_role_dept 主键恢复）", 200, status)
check("切回后又能看到 1 条", 1, call("GET", "/api/activities", bob)[1]["data"]["total"])

call("PUT", "/api/users/%d" % bob_id, admin, {
    "username": "bob", "displayName": "周明远", "roleCodes": ["USER"],
    "departmentId": CS, "email": "bob@feat.plus", "status": 1})
bob = login("bob")
check("收回角色后不能再看日志", 403, call("GET", "/api/activities", bob)[0])

print("--- 重置后回到初始 ---")
status, _ = call("POST", "/api/admin/reset-demo", admin)
check("reset-demo", 200, status)
admin = login("admin")
_, body = call("GET", "/api/users?page=1&pageSize=50", admin)
check("用户回到 12 人", 12, body["data"]["total"])
check("角色回到 4 个", 4, len(call("GET", "/api/roles", admin)[1]["data"]))
alice = login("alice")
check("alice 恢复两个角色", ["DEPT_MANAGER", "USER"], sorted(me(alice)["roleCodes"]))
check("alice 又只能看到 2 人", 2, call("GET", "/api/users", alice)[1]["data"]["total"])
check("bob 回到单角色", ["USER"], me(login("bob"))["roleCodes"])

print("--- 通过 %d / 失败 %d ---" % (passed, failed))
