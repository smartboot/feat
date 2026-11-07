#!/usr/bin/env python3
"""审计日志与通知按人投递的回归用例。

用 urllib 而不是 curl，因为要按 JSON 精确断言数量——之前用 grep 数 '"code"'
会把 RestResult 信封的 code 也数进去，得到「3 个角色显示 4 个」这种假象。
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
            return e.code, {}


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


admin = login("admin")
bob = login("bob")
carol = login("carol")

print("--- 审计日志：基础查询 ---")
status, body = call("GET", "/api/activities?page=1&pageSize=20", admin)
check("GET /api/activities", 200, status)
check("种子日志 4 条", 4, body["data"]["total"])
first = body["data"]["items"][0]
print("  · 最新一条: %s / %s" % (first["title"], first["createdTime"]))

print("--- 审计日志：类型筛选 ---")
_, body = call("GET", "/api/activities?kind=update&page=1&pageSize=50", admin)
check("kind=update", 1, body["data"]["total"])
_, body = call("GET", "/api/activities?kind=system&page=1&pageSize=50", admin)
check("kind=system", 2, body["data"]["total"])
_, body = call("GET", "/api/activities?kind=create&page=1&pageSize=50", admin)
check("kind=create（种子没有）", 0, body["data"]["total"])

print("--- 审计日志：日期筛选 ---")
_, body = call("GET", "/api/activities?from=2026-08-02&to=2026-08-02&page=1&pageSize=50", admin)
check("2026-08-02 当天", 4, body["data"]["total"])
_, body = call("GET", "/api/activities?from=2026-08-03&page=1&pageSize=50", admin)
check("2026-08-03 之后为空", 0, body["data"]["total"])
status, _ = call("GET", "/api/activities?from=2026/08/02", admin)
check("非法日期 400", 400, status)
status, _ = call("GET", "/api/activities?operatorId=abc", admin)
check("非法操作人 400", 400, status)

print("--- 审计日志：写操作被记录 ---")
status, created = call("POST", "/api/users", admin, {
    "username": "audit01", "displayName": "审计测试", "roleCode": "USER",
    "departmentId": 2, "email": "audit01@feat.dev", "status": 1,
})
check("创建用户", 200, status)
new_id = created["data"]["id"]
_, body = call("GET", "/api/activities?kind=create&page=1&pageSize=50", admin)
check("create 日志 +1", 1, body["data"]["total"])
check("操作人姓名已联表", "系统管理员", body["data"]["items"][0]["operatorName"])
_, body = call("GET", "/api/activities?operatorId=1&page=1&pageSize=50", admin)
# 2 条 = 种子里那条「管理员启用了用户 jason」+ 刚才的新建。
check("按操作人筛选", 2, body["data"]["total"])
_, body = call("GET", "/api/activities/operators", admin)
names = [item["name"] for item in body["data"]]
check("操作人下拉 3 人（种子日志也带操作人）", 3, len(names))
check("下拉包含当前管理员", True, "系统管理员" in names)

print("--- 审计日志：权限 ---")
status, _ = call("GET", "/api/activities", bob)
check("bob(USER) 无 audit:read -> 403", 403, status)
status, _ = call("GET", "/api/activities/operators", bob)
check("bob 操作人下拉 403", 403, status)
status, body = call("GET", "/api/activities", carol)
check("carol(AUDITOR) 有 audit:read -> 200", 200, status)
check("carol 也能看到 5 条", 5, body["data"]["total"])
status, _ = call("GET", "/api/activities", None)
check("未登录 401", 401, status)

print("--- 通知：按人隔离 ---")
_, admin_notifications = call("GET", "/api/notifications", admin)
_, bob_notifications = call("GET", "/api/notifications", bob)
_, carol_notifications = call("GET", "/api/notifications", carol)
check("admin 可见 3 条（2 广播 + 1 定向）", 3, len(admin_notifications["data"]))
check("bob 可见 2 条（只有广播）", 2, len(bob_notifications["data"]))
check("carol 可见 3 条（2 广播 + 1 定向）", 3, len(carol_notifications["data"]))
bob_ids = {n["id"] for n in bob_notifications["data"]}
admin_ids = {n["id"] for n in admin_notifications["data"]}
check("admin 看不到 carol 的定向通知", True, 5 not in admin_ids)
check("bob 看得到广播通知", True, 1 in bob_ids and 2 in bob_ids)

def unread(token):
    _, body = call("GET", "/api/dashboard", token)
    return body["data"]["unreadNotifications"]

print("--- 通知：已读状态互不影响 ---")
check("admin 初始未读", 3, unread(admin))
check("bob 初始未读", 2, unread(bob))
status, _ = call("POST", "/api/notifications/1/read", admin)
check("admin 标记广播通知 1 已读", 200, status)
check("admin 未读 -1", 2, unread(admin))
check("bob 未读不受影响", 2, unread(bob))
status, _ = call("POST", "/api/notifications/5/read", admin)
check("admin 标记别人的通知 -> 404", 404, status)
status, _ = call("POST", "/api/notifications/1/read", admin)
check("重复标记幂等", 200, status)
check("重复标记后未读不变", 2, unread(admin))
status, _ = call("POST", "/api/notifications/9999/read", admin)
check("不存在的通知 -> 404", 404, status)
status, _ = call("POST", "/api/notifications/read-all", bob)
check("bob 全部已读", 200, status)
check("bob 未读归零", 0, unread(bob))
check("admin 未读不受影响", 2, unread(admin))

print("--- 重置后回到初始 ---")
status, _ = call("POST", "/api/admin/reset-demo", admin)
check("reset-demo", 200, status)
check("admin 未读恢复 3", 3, unread(admin))
check("bob 未读恢复 2", 2, unread(bob))
_, body = call("GET", "/api/notifications", admin)
check("通知 id 回到 1-5", [1, 2, 3], sorted(n["id"] for n in body["data"]))
_, body = call("GET", "/api/activities?page=1&pageSize=50", admin)
check("日志回到 4 条", 4, body["data"]["total"])
check("日志 id 回到 1-4", [1, 2, 3, 4], sorted(i["id"] for i in body["data"]["items"]))
# 种子日志现在会写 creator，所以重置后下拉应是固定的 3 个人，而不是空。
check("重置后操作人下拉回到 3 人", 3,
      len(call("GET", "/api/activities/operators", admin)[1]["data"]))

print("--- 通过 %d / 失败 %d ---" % (passed, failed))
