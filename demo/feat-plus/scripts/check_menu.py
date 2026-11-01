#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""菜单与权限字典的一致性检查：注册表在后端，路由与图标在前端，两边必须对得上。

为什么需要它：菜单搬到后端之后，「后端配了个前端没有的页面」这一种不一致才变得可能。
它的症状不是报错——点进去会被 catch-all 弹回首页，用户以为点错了。所以要在提交前拦住。

规则：
    1. MenuRegistry 里的每个 path，App.tsx 里必须有对应 Route（详情路由 /users/:userId 归到 /users）
    2. App.tsx 里的每条 Route（不含 / 与 *），都必须在 MenuRegistry 里登记过
       —— 否则就是「有页面没菜单」，只能靠手输 URL 进去
    3. MenuRegistry 里的每个 icon，AppLayout 的 NavIcons 必须有同名 key
       —— 缺了会退化成默认图标，不报错
    4. MenuRegistry 引用的权限点必须存在于 Permission.CATALOG
    5. path 与权限点都不允许重复

只做文本级解析：这个脚本要在没有 JDK、没有 npm 的环境里几秒内跑完。
代价是认不出「用变量拼路径」这类写法——真出现那种写法，本脚本会报「未登记」，
那是好事，菜单路径本来就该是字面量。
"""
import os
import re
import sys

BASE = os.path.join("src", "main", "java", "tech", "smartboot", "feat", "demo", "plus")
HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.join(HERE, "..")

MENU_REGISTRY = os.path.join(ROOT, BASE, "security", "MenuRegistry.java")
PERMISSION = os.path.join(ROOT, BASE, "security", "Permission.java")
APP_TSX = os.path.join(ROOT, "web", "src", "App.tsx")
APP_LAYOUT = os.path.join(ROOT, "web", "src", "app", "AppLayout.tsx")

# 不需要菜单的页面：登录页在未登录时渲染，不可能出现在侧栏。
# 这个名单要尽量短——每加一条都是「有页面没菜单」的例外。
EXEMPT_ROUTES = {"/login"}


def read(path):
    if not os.path.isfile(path):
        sys.exit("未找到文件：%s" % os.path.abspath(path))
    with open(path, encoding="utf-8") as f:
        return f.read()


def squash(text):
    """把换行与连续空白压成单个空格，跨行的 new MenuItem(...) 才好用正则抓。"""
    return re.sub(r"\s+", " ", text)


def permission_constants():
    """常量名 -> 权限点字符串，例如 USER_READ -> user:read。"""
    text = read(PERMISSION)
    return dict(re.findall(r'public static final String (\w+) = "([^"]+)";', text))


def permission_keys():
    """CATALOG 里登记过的权限点，按声明顺序。"""
    constants = permission_constants()
    body = squash(read(PERMISSION))
    keys = []
    for name in re.findall(r'new Entry\(\s*"[^"]*",\s*"[^"]*",\s*(\w+)\s*,', body):
        if name not in constants:
            sys.exit("Permission.CATALOG 引用了未定义的常量：%s" % name)
        keys.append(constants[name])
    return keys


def menu_items():
    """[(path, name, icon, permission_or_None, visible)]"""
    body = squash(read(MENU_REGISTRY))
    pattern = re.compile(
        r'new MenuItem\(\s*"([^"]*)",\s*"([^"]*)",\s*"([^"]*)",\s*"([^"]*)",\s*"([^"]*)",\s*'
        r'(Permission\.(\w+)|""),\s*(\d+),\s*(true|false)\)'
    )
    constants = permission_constants()
    items = []
    for group_key, group_label, path, name, icon, perm_expr, perm_name, sort, visible in pattern.findall(body):
        permission = None
        if perm_name:
            if perm_name not in constants:
                sys.exit("MenuRegistry 引用了未定义的权限点常量：%s" % perm_name)
            permission = constants[perm_name]
        items.append((path, name, icon, permission, visible == "true"))
    if not items:
        sys.exit("MenuRegistry 里没有解析到任何菜单，正则或写法变了？")
    return items


def route_paths():
    body = read(APP_TSX)
    return re.findall(r'<Route\s+path="([^"]+)"', body)


def nav_icon_keys():
    body = read(APP_LAYOUT)
    start = body.find("const NavIcons")
    if start < 0:
        sys.exit("AppLayout 里找不到 NavIcons，图标 key 检查无法进行")
    end = body.find("}", start)
    block = body[start:end]
    return set(re.findall(r'^\s*(\w+):', block, re.MULTILINE))


def normalize(route):
    """去掉详情路由的参数段：/users/:userId -> /users。"""
    return re.sub(r"/:[^/]+$", "", route)


def main():
    items = menu_items()
    keys = permission_keys()
    routes = [r for r in route_paths() if r not in ("/", "*")]
    normalized_routes = {normalize(r) for r in routes}
    icons = nav_icon_keys()

    problems = []

    seen_paths = {}
    for path, name, icon, permission, visible in items:
        if path in seen_paths:
            problems.append("菜单路径重复：%s（%s 与 %s）" % (path, seen_paths[path], name))
        seen_paths[path] = name
        if path not in normalized_routes:
            problems.append("菜单 %s（%s）在 App.tsx 里没有对应 Route" % (path, name))
        if icon not in icons:
            problems.append("菜单 %s 的图标 key「%s」不在 NavIcons 里" % (path, icon))
        if permission is not None and permission not in keys:
            problems.append("菜单 %s 引用了未登记的权限点：%s" % (path, permission))

    for route in sorted(normalized_routes):
        if route in EXEMPT_ROUTES:
            continue
        if route not in seen_paths:
            problems.append("路由 %s 没有任何菜单登记（有页面没菜单，只能手输 URL 进入）" % route)

    if problems:
        print("菜单一致性问题 %d 处：" % len(problems))
        for problem in problems:
            print("  - " + problem)
        return 1

    print("菜单检查通过：%d 个菜单、%d 条路由、%d 个权限点、%d 个图标 key"
          % (len(items), len(normalized_routes), len(keys), len(icons)))
    return 0


if __name__ == "__main__":
    sys.exit(main())
