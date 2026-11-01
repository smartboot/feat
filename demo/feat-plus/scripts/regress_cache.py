#!/usr/bin/env python3
"""缓存回归用例。

验证的不是「有没有缓存」，而是三件容易出错的事：

1. **默认后端是 memory**，而且能通过接口读出来——「以为配了 Redis，其实一直是内存」
   没有任何报错，只能靠这个接口发现；
2. **读路径真的命中缓存**：连续两次拉部门树，第二次必须命中，否则缓存等于没接上；
3. **写路径真的失效缓存**：新建/删除部门后立刻拉树，必须看到变化。
   漏失效不会报错，只会让人看到十分钟前的数据（TTL 兜底之前）。

另外覆盖权限边界：未登录 401、没有 system:write 的角色 403。

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
    """把部门树拍平成 id 集合，用来判断某个部门在不在树里。"""
    acc = acc if acc is not None else []
    for node in nodes:
        acc.append(node["id"])
        flatten(node.get("children") or [], acc)
    return acc


admin = login("admin")
call("POST", "/api/admin/reset-demo", admin)
admin = login("admin")

print("--- 权限边界 ---")
status, _ = call("GET", "/api/system/cache")
check("未登录读取缓存状态", 401, status)
alice = login("alice")
status, body = call("GET", "/api/system/cache", alice)
check("普通角色读取缓存状态", 403, status)

print("--- 默认后端 ---")
status, body = call("GET", "/api/system/cache", admin)
check("缓存状态接口", 200, status)
stats = body.get("data", {})
check("默认后端", "memory", stats.get("provider"))
check_true("计数是可读整数", isinstance(stats.get("hits"), int) and isinstance(stats.get("misses"), int),
           "hits=%s misses=%s" % (stats.get("hits"), stats.get("misses")))
check("命中率字段存在", True, "hitRate" in stats)

print("--- 读路径命中缓存 ---")
before = call("GET", "/api/system/cache", admin)[1]["data"]
call("GET", "/api/departments", admin)
call("GET", "/api/departments", admin)
after = call("GET", "/api/system/cache", admin)[1]["data"]
gained_hits = after["hits"] - before["hits"]
check_true("两次拉部门树至少命中一次", gained_hits >= 1, "hits +%s" % gained_hits)
check_true("未命中次数没有同步翻倍", (after["misses"] - before["misses"]) < 2,
           "misses +%s" % (after["misses"] - before["misses"]))

print("--- 写路径失效缓存 ---")
created = "缓存测试部"
status, body = call("POST", "/api/departments", admin,
                    {"name": created, "code": "CACHE_QA", "parentId": None, "description": "缓存失效用例"})
check("新建部门", 200, status)
new_id = body.get("data", {}).get("id")
status, body = call("GET", "/api/departments", admin)
ids = flatten(body.get("data", []))
check_true("新建后立刻出现在部门树里", new_id in ids, "id=%s" % new_id)

status, _ = call("DELETE", "/api/departments/%s" % new_id, admin)
check("删除部门", 200, status)
status, body = call("GET", "/api/departments", admin)
ids = flatten(body.get("data", []))
check_true("删除后立刻从部门树里消失", new_id not in ids, "id=%s" % new_id)

print("--- 重置演示数据后缓存不能留旧树 ---")
status, body = call("POST", "/api/departments", admin,
                    {"name": "重置前临时部", "code": "TMP_QA", "parentId": None})
temp_id = body.get("data", {}).get("id")
call("POST", "/api/admin/reset-demo", admin)
admin = login("admin")
status, body = call("GET", "/api/departments", admin)
ids = flatten(body.get("data", []))
check_true("重置后旧部门不再出现", temp_id not in ids, "temp=%s" % temp_id)
check_true("重置后部门树非空", len(ids) > 0, "%s 个节点" % len(ids))

print()
print("通过 %d / 失败 %d" % (passed, failed))
raise SystemExit(1 if failed else 0)
