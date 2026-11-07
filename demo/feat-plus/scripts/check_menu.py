#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""菜单一致性检查：页面白名单在后端、路由在前端、种子数据在 seed 包，三处必须对得上。

菜单搬进数据库之后，「配一个不存在的页面」从「不可能」变成了「填错一个字符串」。
它的症状不是报错——侧栏有这一条，点进去被 catch-all 弹回首页，数据看起来完全正常。
所以运行期由 MenuService 拒绝白名单外的路径，提交前由这个脚本保证三处一致。

规则：
    1. MenuRoutes 里的每个 path，App.tsx 里必须有对应 Route（详情路由 /users/:userId 归到 /users）
    2. App.tsx 里的每条 Route（不含 / 与 *），都必须在 MenuRoutes 里登记过
       —— 否则就是「有页面没菜单」，只能靠手输 URL 进去
    3. 种子菜单的 path 必须全部在 MenuRoutes 里，且不重复
    4. 种子菜单的 icon 必须在 web/src/lib/icons.ts 的 NAV_ICONS 里
       —— 缺了会退化成默认图标，不报错
    5. 种子菜单的 permission 必须存在于 Permission.CATALOG（空串除外）
    6. MenuRoutes 里的每个页面都要有种子菜单，否则首次启动的侧栏会缺项

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

MENU_ROUTES = os.path.join(ROOT, BASE, "security", "MenuRoutes.java")
PERMISSION = os.path.join(ROOT, BASE, "security", "Permission.java")
SEED_DATA = os.path.join(ROOT, BASE, "seed", "SeedData.java")
APP_TSX = os.path.join(ROOT, "web", "src", "App.tsx")
NAV_ICONS = os.path.join(ROOT, "web", "src", "lib", "icons.ts")

# 不需要菜单的页面：登录页在未登录时渲染，不可能出现在侧栏。
# 这个名单要尽量短——每加一条都是「有页面没菜单」的例外。
EXEMPT_ROUTES = {"/login"}


def read(path):
    if not os.path.isfile(path):
        sys.exit("未找到文件：%s" % os.path.abspath(path))
    with open(path, encoding="utf-8") as f:
        return f.read()


def squash(text):
    """把换行与连续空白压成单个空格，跨行的 new SeedMenu(...) 才好用正则抓。"""
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


def route_whitelist():
    """[(常量名, 路径)]，保持声明顺序。"""
    pairs = re.findall(r'public static final String (\w+) = "([^"]+)";', read(MENU_ROUTES))
    if not pairs:
        sys.exit("MenuRoutes 里没有解析到任何路径常量，正则或写法变了？")
    return pairs


def seed_menus():
    """[(path, name, icon, permission_or_None)]"""
    body = squash(read(SEED_DATA))
    pattern = re.compile(
        r'new SeedMenu\(\s*"([^"]*)",\s*"([^"]*)",\s*(-?\d+),\s*'
        r'(?:MenuRoutes\.(\w+)|"([^"]*)"),\s*"([^"]*)",\s*"([^"]*)",\s*'
        r'(?:Permission\.(\w+)|"([^"]*)"),\s*(-?\d+),\s*(true|false)\)'
    )
    constants = permission_constants()
    paths = dict(route_whitelist())
    items = []
    for (_, _, _, path_const, path_literal, name, icon, perm_const, perm_literal,
         _, _) in pattern.findall(body):
        if path_const:
            if path_const not in paths:
                sys.exit("SeedData 引用了未定义的路径常量：MenuRoutes.%s" % path_const)
            path = paths[path_const]
        else:
            path = path_literal
        if perm_const:
            if perm_const not in constants:
                sys.exit("SeedData 引用了未定义的权限点常量：%s" % perm_const)
            permission = constants[perm_const]
        else:
            permission = perm_literal or None
        items.append((path, name, icon, permission))
    if not items:
        sys.exit("SeedData.menus() 里没有解析到任何菜单，正则或写法变了？")
    return items


def app_routes():
    return re.findall(r'<Route\s+path="([^"]+)"', read(APP_TSX))


def nav_icon_keys():
    body = read(NAV_ICONS)
    start = body.find("export const NAV_ICONS")
    if start < 0:
        sys.exit("web/src/lib/icons.ts 里找不到 NAV_ICONS，图标 key 检查无法进行")
    end = body.find("\n}", start)
    block = body[start:end]
    return set(re.findall(r'^\s*(\w+):', block, re.MULTILINE))


def normalize(route):
    """去掉详情路由的参数段：/users/:userId -> /users。"""
    return re.sub(r"/:[^/]+$", "", route)


def main():
    whitelist = [path for _, path in route_whitelist()]
    items = seed_menus()
    keys = permission_keys()
    routes = [r for r in app_routes() if r not in ("/", "*")]
    normalized_routes = {normalize(r) for r in routes}
    icons = nav_icon_keys()

    problems = []

    seen_paths = {}
    for path, name, icon, permission in items:
        if path in seen_paths:
            problems.append("种子菜单路径重复：%s（%s 与 %s）" % (path, seen_paths[path], name))
        seen_paths[path] = name
        if path not in whitelist:
            problems.append("种子菜单 %s（%s）不在 MenuRoutes 白名单里" % (path, name))
        if path not in normalized_routes:
            problems.append("种子菜单 %s（%s）在 App.tsx 里没有对应 Route" % (path, name))
        if icon not in icons:
            problems.append("种子菜单 %s 的图标 key「%s」不在 NAV_ICONS 里" % (path, icon))
        if permission is not None and permission not in keys:
            problems.append("种子菜单 %s 引用了未登记的权限点：%s" % (path, permission))

    for path in whitelist:
        if path not in normalized_routes:
            problems.append("MenuRoutes 里的 %s 在 App.tsx 里没有对应 Route" % path)

    for route in sorted(normalized_routes):
        if route in EXEMPT_ROUTES:
            continue
        if route not in whitelist:
            problems.append("路由 %s 不在 MenuRoutes 白名单里（后端配菜单时选不到这个页面）" % route)
        if route not in seen_paths:
            problems.append("路由 %s 没有任何种子菜单（首次启动时侧栏没有这一项）" % route)

    if problems:
        print("菜单一致性问题 %d 处：" % len(problems))
        for problem in problems:
            print("  - " + problem)
        return 1

    print("菜单检查通过：白名单 %d 个页面、种子菜单 %d 条、权限点 %d 个、图标 key %d 个"
          % (len(whitelist), len(items), len(keys), len(icons)))
    return 0


if __name__ == "__main__":
    sys.exit(main())
