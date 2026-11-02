#!/usr/bin/env python3
"""错误契约回归用例。

验证错误响应是「程序可消费的契约」而不只是一段人话：

1. **响应体 code 与 HTTP 状态码一致**。RestResult.fail 默认写 500，不纠正的话
   HTTP 403 的响应体里 code 也是 500，按 body 判断的调用方全部误判；
2. **每个响应都带 X-Trace-Id**（成功与失败都是），且各请求互不相同——
   前端报障带上它，服务端日志就能按 id 定位；
3. **探针语义**：/api/health 免登录且不查库（数据库挂了仍回 200，进程重启救不了它），
   /api/health/ready 做真实数据库往返，不通时非 200。

运行前需先启动服务（默认 8080）。脚本只读与触发校验失败，不落任何数据。
"""
import json
import re
import urllib.error
import urllib.request

BASE = "http://127.0.0.1:8080"
# 沙箱设了 HTTP_PROXY，访问本机必须显式绕过，否则会拿到代理返回的 502。
OPENER = urllib.request.build_opener(urllib.request.ProxyHandler({}))

passed = 0
failed = 0
trace_ids = set()

TRACE_RE = re.compile(r"^[0-9a-f]{16}$")


def call(method, path, token=None, body=None):
    """返回 (status, headers, body_json)。headers 供 X-Trace-Id 断言用。"""
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(BASE + path, data=data, method=method)
    req.add_header("Accept", "application/json")
    if data is not None:
        req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    try:
        with OPENER.open(req) as resp:
            return resp.status, dict(resp.headers), json.loads(resp.read().decode() or "{}")
    except urllib.error.HTTPError as e:
        raw = e.read().decode()
        try:
            return e.code, dict(e.headers), json.loads(raw or "{}")
        except json.JSONDecodeError:
            return e.code, dict(e.headers), {"message": raw[:120]}


def login(username, password="admin123"):
    status, _, body = call("POST", "/api/auth/login", body={"username": username, "password": password})
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


def traced(name, status, headers):
    """每个响应都必须带格式合法且互不重复的 X-Trace-Id。"""
    global passed, failed
    trace = headers.get("X-Trace-Id")
    if trace and TRACE_RE.match(trace) and trace not in trace_ids:
        trace_ids.add(trace)
        passed += 1
        print("  OK   %s -> %s" % (name, trace))
    else:
        failed += 1
        print("  FAIL %s  trace=%r 重复=%s" % (name, trace, trace in trace_ids if trace else "-"))


def expect_error(name, status, headers, body, expected_status):
    """错误响应的三连断言：HTTP 状态、body code 与之一致、可读 message。"""
    check(name + "（HTTP 状态）", expected_status, status)
    check(name + "（body.code 与 HTTP 状态一致）", expected_status, body.get("code"))
    check_true(name + "（success=false）", body.get("success") is False)
    check_true(name + "（message 可读）", isinstance(body.get("message"), str) and len(body.get("message", "")) > 0,
               body.get("message"))
    traced(name + "（X-Trace-Id）", status, headers)


print("== 探针（免登录） ==")
status, headers, body = call("GET", "/api/health")
check("存活探针免登录 200", 200, status)
check("status=up", "up", body.get("data", {}).get("status"))
check("存活探针不查库（db=skipped）", "skipped", body.get("data", {}).get("db"))
traced("存活探针 X-Trace-Id", status, headers)

status, headers, body = call("GET", "/api/health/ready")
check("就绪探针免登录 200", 200, status)
check("数据库往返成功（db=ok）", "ok", body.get("data", {}).get("db"))
traced("就绪探针 X-Trace-Id", status, headers)

status2, headers2, _ = call("GET", "/api/health")
traced("两次请求 traceId 不同", status2, headers2)

print("== 401 / 403 / 404 / 400 契约 ==")
status, headers, body = call("GET", "/api/auth/me")
expect_error("未登录访问 /me", status, headers, body, 401)

status, headers, body = call("GET", "/api/auth/me", token="invalid-token")
expect_error("无效令牌访问 /me", status, headers, body, 401)

# 密码错误是 400 不是 401：前端的 request() 把一切 401 当「令牌失效」处理（清 token、
# 踢回登录页），登录页自己的失败走 400 不会误触发那套逻辑——契约保持现状并在这里钉住。
status, headers, body = call("POST", "/api/auth/login", body={"username": "admin", "password": "wrong"})
expect_error("密码错误", status, headers, body, 400)

bob = login("bob")
status, headers, body = call("GET", "/api/users", token=bob)
expect_error("无权限列用户（bob）", status, headers, body, 403)

admin = login("admin")
status, headers, body = call("GET", "/api/users/999999", token=admin)
expect_error("查询不存在的用户", status, headers, body, 404)

status, headers, body = call("GET", "/api/users?keyword=admin", token=admin)
rows = body.get("data", {}).get("items", [])
me_row = rows[0] if rows else None
duplicate = {
    "username": me_row.get("username"),
    "displayName": "冒名顶替",
    "email": "dup-%d@example.com" % me_row.get("id"),
    "departmentId": me_row.get("departmentId"),
    "roleCodes": me_row.get("roleCodes") or ["USER"],
    "status": 1,
}
status, headers, body = call("POST", "/api/users", token=admin, body=duplicate)
expect_error("创建用户重名（admin）", status, headers, body, 400)

status, headers, body = call("POST", "/api/users", token=admin, body={})
expect_error("创建用户空入参", status, headers, body, 400)

print("== 成功响应同样带 traceId ==")
status, headers, _ = call("GET", "/api/auth/me", token=admin)
traced("登录态成功响应 X-Trace-Id", status, headers)

check_true("全部请求 traceId 互不重复", len(trace_ids) >= 11, len(trace_ids))

print("\n通过 %d 项，失败 %d 项" % (passed, failed))
raise SystemExit(1 if failed else 0)
