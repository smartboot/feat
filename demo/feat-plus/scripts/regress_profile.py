#!/usr/bin/env python3
"""个人中心回归用例。

覆盖三件事：
1. 任何登录用户都能读改自己的资料，**不需要任何权限点**（普通成员改密码不该找管理员）；
2. 字段校验：昵称空/超长、邮箱格式、密码长度、新旧密码相同；
3. 改密码后其它会话立即失效、当前会话保留、旧密码不能再登录。

运行前需先启动服务（默认 8080），脚本开头与结尾都会重置演示数据。
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


admin = login("admin")
call("POST", "/api/admin/reset-demo", admin)
admin = login("admin")

print("--- 读取自己的资料 ---")
check("未登录读取 401", 401, call("GET", "/api/profile")[0])

bob = login("bob")
status, body = call("GET", "/api/profile", bob)
check("普通成员能读自己的资料", 200, status)
data = body["data"]
check("用户名", "bob", data["username"])
check("昵称", "周明远", data["displayName"])
check("邮箱", "bob@feat.plus", data["email"])
check("部门名", "客户成功部", data["departmentName"])
check("角色名", ["普通成员"], data["roleNames"])
check("数据范围说明", "仅本人", data["scopeDescription"])
check("创建时间不为空", True, bool(data["createdTime"]))
# 普通成员没有 user:read，却能读自己的资料——这是本模块的核心诉求。
check("普通成员没有 user:read", False, "user:read" in
      call("GET", "/api/auth/me", bob)[1]["data"]["permissions"])
check("不返回密码哈希", False, any("password" in key.lower() for key in data.keys()))

print("--- 修改自己的资料 ---")
status, body = call("PUT", "/api/profile", bob, {"displayName": "周明远（改）", "email": "bob2@feat.plus"})
check("改昵称与邮箱", 200, status)
check("返回新昵称", "周明远（改）", body["data"]["displayName"])
check("/me 同步昵称", "周明远（改）", call("GET", "/api/auth/me", bob)[1]["data"]["displayName"])
check("管理员看到的列表也变了", "周明远（改）",
      call("GET", "/api/users?keyword=bob", admin)[1]["data"]["items"][0]["displayName"])

check("空昵称被拒", 400, call("PUT", "/api/profile", bob, {"displayName": "", "email": "b@c.d"})[0])
check("全空格昵称被拒", 400, call("PUT", "/api/profile", bob, {"displayName": "   ", "email": ""})[0])
check("昵称超 32 字被拒", 400,
      call("PUT", "/api/profile", bob, {"displayName": "昵" * 33, "email": ""})[0])
check("邮箱缺 @ 被拒", 400,
      call("PUT", "/api/profile", bob, {"displayName": "周明远", "email": "not-an-email"})[0])
check("邮箱域名缺后缀被拒", 400,
      call("PUT", "/api/profile", bob, {"displayName": "周明远", "email": "a@b"})[0])
check("邮箱可以清空", 200,
      call("PUT", "/api/profile", bob, {"displayName": "周明远", "email": ""})[0])
check("清空后读回为空", "", call("GET", "/api/profile", bob)[1]["data"]["email"])

# 资料接口没有 id 参数，只能改自己；即使强行传 id 也不应该生效。
call("PUT", "/api/profile", bob, {"displayName": "周明远", "email": "bob@feat.plus", "id": 1,
                                  "username": "admin", "roleCodes": ["ADMIN"]})
check("传 id / username / roleCodes 改不动别人", "系统管理员",
      call("GET", "/api/users/1", admin)[1]["data"]["displayName"])
check("自己的角色没被改动", ["普通成员"], call("GET", "/api/profile", bob)[1]["data"]["roleNames"])

print("--- 修改自己的密码 ---")
bob_token = login("bob")
bob_other = login("bob")
check("同一账号两个会话", True, bob_token is not None and bob_other is not None)

check("旧密码错误被拒", 400,
      call("POST", "/api/profile/password", bob_token,
           {"oldPassword": "wrong", "newPassword": "newpass123"})[0])
check("新密码不足 8 位被拒", 400,
      call("POST", "/api/profile/password", bob_token,
           {"oldPassword": "admin123", "newPassword": "1234567"})[0])
check("新密码与旧密码相同被拒", 400,
      call("POST", "/api/profile/password", bob_token,
           {"oldPassword": "admin123", "newPassword": "admin123"})[0])
check("缺旧密码被拒", 400,
      call("POST", "/api/profile/password", bob_token, {"newPassword": "newpass123"})[0])

status, body = call("POST", "/api/profile/password", bob_token,
                    {"oldPassword": "admin123", "newPassword": "newpass123"})
check("改密码成功", 200, status)
# 不写死具体条数：令牌存在进程内存里，跨多次脚本运行会累积，
# 而「另一个会话确实失效」才是真正要断言的东西（下一行）。
check("返回被踢掉的会话数至少 1", True, body["data"] >= 1)
check("当前会话仍然有效", 200, call("GET", "/api/auth/me", bob_token)[0])
check("另一个会话已失效", 401, call("GET", "/api/auth/me", bob_other)[0])
check("旧密码不能登录", None, login("bob", "admin123"))
check("新密码可以登录", True, login("bob", "newpass123") is not None)

print("--- 重置回初始 ---")
status, _ = call("POST", "/api/admin/reset-demo", admin)
check("reset-demo", 200, status)
admin = login("admin")
check("admin 仍可登录", True, admin is not None)
bob = login("bob")
check("bob 密码回到 admin123", True, bob is not None)
check("bob 昵称已恢复", "周明远", call("GET", "/api/profile", bob)[1]["data"]["displayName"])
check("bob 邮箱已恢复", "bob@feat.plus", call("GET", "/api/profile", bob)[1]["data"]["email"])

print("--- 通过 %d / 失败 %d ---" % (passed, failed))
