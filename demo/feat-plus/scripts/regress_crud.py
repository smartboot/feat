#!/usr/bin/env python3
"""用户 / 角色 / 部门的基础读写回归。

这一份盯的是「改造前就该成立」的行为：唯一性、软删除语义、内置保护、部门树三约束。
多角色与数据权限单独放在 regress_scope.py。
"""
import json
import urllib.request

BASE = "http://127.0.0.1:8080"
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

_, tree = call("GET", "/api/departments", admin)
DEPTS = {}


def walk(nodes):
    for node in nodes:
        DEPTS[node["code"]] = node["id"]
        walk(node["children"])


walk(tree["data"])
ROLES = {r["code"]: r["id"] for r in call("GET", "/api/roles", admin)[1]["data"]}

print("--- 鉴权 ---")
check("未登录访问用户列表 401", 401, call("GET", "/api/users")[0])
check("错误密码 400", 400, call("POST", "/api/auth/login", body={"username": "admin", "password": "x"})[0])
check("空用户名 400", 400, call("POST", "/api/auth/login", body={"username": "", "password": "x"})[0])
auditor = login("carol")
check("审计员有 user:read -> 200", 200, call("GET", "/api/users", auditor)[0])
check("审计员无 user:write -> 403", 403, call("POST", "/api/users", auditor, {
    "username": "x1", "displayName": "x", "roleCodes": ["USER"],
    "departmentId": DEPTS["CS"], "email": "x1@feat.plus", "status": 1})[0])

print("--- 用户：分页与筛选 ---")
_, body = call("GET", "/api/users?page=1&pageSize=5", admin)
check("pageSize=5 返回 5 条", 5, len(body["data"]["items"]))
check("total 仍为 12", 12, body["data"]["total"])
_, body = call("GET", "/api/users?keyword=ali", admin)
check("关键字 alice", ["alice"], [i["username"] for i in body["data"]["items"]])
_, body = call("GET", "/api/users?status=0", admin)
check("停用账号 2 个", 2, body["data"]["total"])
_, body = call("GET", "/api/users?roleCode=USER", admin)
check("USER 角色 9 人", 9, body["data"]["total"])
_, body = call("GET", "/api/users?departmentId=%d" % DEPTS["TECH"], admin)
check("技术中心含下级共 4 人", 4, body["data"]["total"])
_, body = call("GET", "/api/users?pageSize=500", admin)
check("pageSize 上限收敛到 200", 200, body["data"]["pageSize"])

print("--- 用户：写入与唯一性 ---")
status, created = call("POST", "/api/users", admin, {
    "username": "crud01", "displayName": "回归用户", "roleCodes": ["USER"],
    "departmentId": DEPTS["CS"], "email": "crud01@feat.plus", "status": 1})
check("创建用户", 200, status)
uid = created["data"]["id"]
check("默认返回角色编码", ["USER"], created["data"]["roleCodes"])
check("重复用户名 400", 400, call("POST", "/api/users", admin, {
    "username": "crud01", "displayName": "x", "roleCodes": ["USER"],
    "departmentId": DEPTS["CS"], "email": "crud02@feat.plus", "status": 1})[0])
check("重复邮箱 400", 400, call("POST", "/api/users", admin, {
    "username": "crud02", "displayName": "x", "roleCodes": ["USER"],
    "departmentId": DEPTS["CS"], "email": "crud01@feat.plus", "status": 1})[0])
check("缺角色 400", 400, call("POST", "/api/users", admin, {
    "username": "crud03", "displayName": "x", "roleCodes": [],
    "departmentId": DEPTS["CS"], "email": "crud03@feat.plus", "status": 1})[0])
check("不存在的角色 400", 400, call("POST", "/api/users", admin, {
    "username": "crud04", "displayName": "x", "roleCodes": ["NOPE"],
    "departmentId": DEPTS["CS"], "email": "crud04@feat.plus", "status": 1})[0])
check("不存在的部门 400", 400, call("POST", "/api/users", admin, {
    "username": "crud05", "displayName": "x", "roleCodes": ["USER"],
    "departmentId": 9999, "email": "crud05@feat.plus", "status": 1})[0])

status, updated = call("PUT", "/api/users/%d" % uid, admin, {
    "username": "crud01", "displayName": "回归用户改", "roleCodes": ["AUDITOR"],
    "departmentId": DEPTS["FIN"], "email": "crud01@feat.plus", "status": 1})
check("更新用户", 200, status)
check("改派角色生效", ["AUDITOR"], updated["data"]["roleCodes"])
check("改派部门生效", DEPTS["FIN"], updated["data"]["departmentId"])
check("停用用户", 200, call("POST", "/api/users/%d/status" % uid, admin, {"status": 0})[0])
check("停用后 status=0", 0, call("GET", "/api/users/%d" % uid, admin)[1]["data"]["status"])

check("停用账号不能登录", None, login("crud01"))
check("重新启用", 200, call("POST", "/api/users/%d/status" % uid, admin, {"status": 1})[0])
status, pw = call("POST", "/api/users/%d/reset-password" % uid, admin)
check("重置密码", 200, status)
check("临时密码可登录", True, login("crud01", pw["data"]["temporaryPassword"]) is not None)

print("--- 用户：删除语义 ---")
check("删除用户", 200, call("DELETE", "/api/users/%d" % uid, admin)[0])
check("删除后详情 404", 404, call("GET", "/api/users/%d" % uid, admin)[0])
_, body = call("GET", "/api/users?page=1&pageSize=50", admin)
check("列表回到 12 人", 12, body["data"]["total"])
status, recreated = call("POST", "/api/users", admin, {
    "username": "crud01", "displayName": "重建同名", "roleCodes": ["USER"],
    "departmentId": DEPTS["CS"], "email": "crud01@feat.plus", "status": 1})
check("软删除后同名账号可重建", 200, status)
check("不能删除自己 400", 400, call("DELETE", "/api/users/1", admin)[0])

print("--- 角色 ---")
status, role = call("POST", "/api/roles", admin, {
    "code": "CRUD_ROLE", "name": "回归角色", "description": "d",
    "permissions": ["dashboard:read"], "status": 1})
check("创建角色", 200, status)
role_id = role["data"]["id"]
check("默认数据范围=全部", 1, role["data"]["departmentScope"])
check("重复编码 400", 400, call("POST", "/api/roles", admin, {
    "code": "CRUD_ROLE", "name": "另一个", "description": "",
    "permissions": [], "status": 1})[0])
check("非法编码 400", 400, call("POST", "/api/roles", admin, {
    "code": "1bad", "name": "非法", "description": "",
    "permissions": [], "status": 1})[0])
check("未知权限点 400", 400, call("POST", "/api/roles", admin, {
    "code": "CRUD_BAD", "name": "未知权限", "description": "",
    "permissions": ["hack:all"], "status": 1})[0])
check("更新角色", 200, call("PUT", "/api/roles/%d" % role_id, admin, {
    "name": "回归角色改", "description": "d2", "permissions": ["dashboard:read", "user:read"],
    "status": 1})[0])
check("停用角色", 200, call("POST", "/api/roles/%d/status" % role_id, admin, {"status": 0})[0])
check("删除无人角色", 200, call("DELETE", "/api/roles/%d" % role_id, admin)[0])
check("内置角色不可停用 400", 400, call("POST", "/api/roles/%d/status" % ROLES["USER"], admin, {"status": 0})[0])
check("在用角色不可删 400", 400, call("DELETE", "/api/roles/%d" % ROLES["USER"], admin)[0])

print("--- 部门树 ---")
status, parent = call("POST", "/api/departments", admin, {
    "parentId": 0, "code": "CRUD_T", "name": "回归中心", "sortOrder": 1,
    "description": "", "status": 1})
check("创建根部门", 200, status)
parent_id = parent["data"]["id"]
status, child = call("POST", "/api/departments", admin, {
    "parentId": parent_id, "code": "CRUD_T_C", "name": "回归子部门", "sortOrder": 1,
    "description": "", "status": 1})
check("创建子部门", 200, status)
child_id = child["data"]["id"]
check("同父下重名 400", 400, call("POST", "/api/departments", admin, {
    "parentId": parent_id, "code": "CRUD_T_C2", "name": "回归子部门", "sortOrder": 2,
    "description": "", "status": 1})[0])
check("不能挂到自己的子树 400", 400, call("PUT", "/api/departments/%d" % parent_id, admin, {
    "parentId": child_id, "code": "CRUD_T", "name": "回归中心", "sortOrder": 1,
    "description": "", "status": 1})[0])
check("有子部门不能删 400", 400, call("DELETE", "/api/departments/%d" % parent_id, admin)[0])
check("删除子部门", 200, call("DELETE", "/api/departments/%d" % child_id, admin)[0])
check("有成员的部门不能删 400", 400, call("DELETE", "/api/departments/%d" % DEPTS["CS"], admin)[0])
check("删除空部门", 200, call("DELETE", "/api/departments/%d" % parent_id, admin)[0])

print("--- 重置 ---")
check("reset-demo", 200, call("POST", "/api/admin/reset-demo", admin)[0])
check("用户回到 12", 12, call("GET", "/api/users", admin)[1]["data"]["total"])
check("角色回到 4", 4, len(call("GET", "/api/roles", admin)[1]["data"]))

print("--- 通过 %d / 失败 %d ---" % (passed, failed))
