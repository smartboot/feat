#!/usr/bin/env python3
"""事务回归：证明「多语句写」真的是原子的。

事务是那种「做错了不报错」的机制——Mapper 混用、内层开了新会话、驱动隐式提交，
任何一种错法都表现为接口返回 200，只有等某次上线少提交一笔或者多出脏数据才会暴露。
所以这里的断言分两类：

1. **回滚真的发生了**：`/api/system/selftest` 在一个事务里写一行再抛异常，
   事务外回查必须查不到。只回滚不看残留是不够的——「根本没进事务」也会返回成功。
2. **业务写的三处一起生效**：建一个用户会写 sys_user / sys_user_role / sys_activity，
   成功时三处都得在；失败时一处都不能留（尤其是审计日志，它现在和业务写在同一个事务里）。

第二类不看表，只看接口可见的结果：用户列表里有没有这个人、角色对不对、审计日志条数变没变。
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


def check_true(name, condition, detail=""):
    global passed, failed
    if condition:
        passed += 1
        print("  OK   %s%s" % (name, (" -> " + str(detail)) if detail != "" else ""))
    else:
        failed += 1
        print("  FAIL %s  %s" % (name, detail))


def flatten(nodes, acc=None):
    acc = acc if acc is not None else []
    for node in nodes:
        acc.append(node)
        flatten(node.get("children") or [], acc)
    return acc


def user_total(token):
    return call("GET", "/api/users?page=1&pageSize=1", token)[1]["data"]["total"]


def activity_total(token):
    return call("GET", "/api/activities?page=1&pageSize=1", token)[1]["data"]["total"]


admin = login("admin")
call("POST", "/api/admin/reset-demo", admin)
admin = login("admin")

DEPTS = {node["code"]: node["id"] for node in flatten(call("GET", "/api/departments", admin)[1]["data"])}

print("--- 自检接口的权限边界 ---")
check("未登录访问自检", 401, call("GET", "/api/system/selftest")[0])
alice = login("alice")
check("普通角色访问自检", 403, call("GET", "/api/system/selftest", alice)[0])

print("--- 回滚真的发生了 ---")
status, body = call("GET", "/api/system/selftest", admin)
check("自检接口", 200, status)
probes = body.get("data") or []
check_true("返回了 transaction.rollback 探针", any(p.get("name") == "transaction.rollback" for p in probes),
           [p.get("name") for p in probes])
probe = next((p for p in probes if p.get("name") == "transaction.rollback"), {})
check("回滚探针通过", True, probe.get("ok"))
print("       说明：%s" % probe.get("detail"))

ids = [node["id"] for node in flatten(call("GET", "/api/departments", admin)[1]["data"])]
selftest_rows = [node for node in flatten(call("GET", "/api/departments", admin)[1]["data"])
                 if str(node.get("name", "")).startswith("__selftest_")]
check_true("自检没有在部门表里留下残留行", len(selftest_rows) == 0, "残留 %s 行" % len(selftest_rows))
check_true("部门树长度没有被自检改变", len(ids) > 0, "%s 个节点" % len(ids))

print("--- 多语句写：成功时三处都在 ---")
before_users = user_total(admin)
before_activity = activity_total(admin)
status, body = call("POST", "/api/users", admin, {
    "username": "tx_user", "displayName": "事务用例", "email": "tx_user@feat.plus",
    "departmentId": DEPTS["CS"], "roleCodes": ["USER", "AUDITOR"], "status": 1})
check("建号成功", 200, status)
new_id = body.get("data", {}).get("id")
check("用户数 +1", before_users + 1, user_total(admin))
_, body = call("GET", "/api/users/%s" % new_id, admin)
check("角色两个都写进去了", ["AUDITOR", "USER"], sorted(body["data"]["roleCodes"]))
check("审计日志 +1", before_activity + 1, activity_total(admin))
_, body = call("GET", "/api/activities?page=1&pageSize=1", admin)
first = (body["data"]["items"] or [{}])[0]
check("最新一条日志是建号", "create", first.get("kind"))
check_true("日志里带着新账号的名字", "事务用例" in (first.get("title") or ""), first.get("title"))

print("--- 多语句写：失败时一处都不留 ---")
before_users = user_total(admin)
before_activity = activity_total(admin)
status, _ = call("POST", "/api/users", admin, {
    "username": "tx_user", "displayName": "重名", "email": "dup@feat.plus",
    "departmentId": DEPTS["CS"], "roleCodes": ["USER"], "status": 1})
check("用户名重复被拒", 400, status)
check("用户数没变", before_users, user_total(admin))
check("审计日志没变", before_activity, activity_total(admin))

before_activity = activity_total(admin)
status, _ = call("POST", "/api/users", admin, {
    "username": "tx_bad_role", "displayName": "错角色", "email": "bad@feat.plus",
    "departmentId": DEPTS["CS"], "roleCodes": ["NOPE"], "status": 1})
check("未知角色被拒", 400, status)
check("审计日志没变（失败的操作不写审计）", before_activity, activity_total(admin))

print("--- 部门：重名失败不留半截数据 ---")
dept_before = len(flatten(call("GET", "/api/departments", admin)[1]["data"]))
status, _ = call("POST", "/api/departments", admin, {
    "name": "平台研发部", "code": "DUP_DEPT", "parentId": DEPTS["TECH"]})
check("同级重名被拒", 400, status)
dept_after = len(flatten(call("GET", "/api/departments", admin)[1]["data"]))
check("部门数没变", dept_before, dept_after)

print("--- 事务里删干净：删号会同时废掉角色关联 ---")
status, _ = call("DELETE", "/api/users/%s" % new_id, admin)
check("删号成功", 200, status)
check("用户数回到初始", before_users - 1, user_total(admin))
status, _ = call("GET", "/api/users/%s" % new_id, admin)
check("删掉的号查不到", 404, status)

print()
print("通过 %d / 失败 %d" % (passed, failed))
raise SystemExit(1 if failed else 0)
