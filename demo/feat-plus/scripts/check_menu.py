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
    7. 动态页面例外：/p/:pageKey 是唯一的动态路由，不进白名单（合法性由 sys_page 决定），
       但挂 /p/xxx 的种子菜单必须满足三条替代规则：
       a) App.tsx 里存在 /p/:pageKey 这条动态路由
       b) pageKey 格式合法（^/p/[a-z0-9_-]{1,32}$）
       c) pageKey 在 src/main/resources/pages/pages.json 里有对应配置——
          菜单 ↔ 页面配置 ↔ 动态路由三方一致

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
PAGES_JSON = os.path.join(ROOT, "src", "main", "resources", "pages", "pages.json")
APP_TSX = os.path.join(ROOT, "web", "src", "App.tsx")
NAV_ICONS = os.path.join(ROOT, "web", "src", "lib", "icons.ts")

# 不需要菜单的页面：登录页在未登录时渲染，不可能出现在侧栏。
# 这个名单要尽量短——每加一条都是「有页面没菜单」的例外。
EXEMPT_ROUTES = {"/login"}

# 动态页面：唯一的参数化路由，页面合法性由 sys_page 的 pageKey 决定，
# 走规则 7 的三方一致性（替代「白名单 + 种子菜单」两方一致性）。
DYNAMIC_ROUTE = "/p/:pageKey"
DYNAMIC_PATH_RE = re.compile(r"^/p/[a-z0-9_-]{1,32}$")

# 与 PageService.KEY_PATTERN / DataInitializer.KEY_PATTERN 同源：pageKey 同时是
# 菜单 path 段、前端拉配置的键、sys_page 的唯一键，三处必须同一套规则。
PAGE_KEY_RE = re.compile(r"^[a-z0-9_-]{1,32}$")


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


def seed_pages():
    """pages/pages.json 里的 pageKey 列表。

    页面配置是「一份 JSON 数组，首次启动由 DataInitializer 导入 sys_page」。
    导入路径只做结构校验（它跑在 @PostConstruct 里，比数据源注册表还早，拿不到字段声明），
    所以这里把「启动就炸」和「页面白屏」这两类问题钉在提交前：
      - 顶层是非空数组，pageKey 合法、不重复
      - title / datasourceKey 非空
      - list.columns 非空、form.fields 存在（只读页面写空数组）
      - 列与筛选的 key 各自不重复（重复会让 React 复用节点，表现为「改一列动两列」）
    """
    import json

    if not os.path.isfile(PAGES_JSON):
        sys.exit("未找到页面配置文件：%s" % os.path.abspath(PAGES_JSON))
    try:
        with open(PAGES_JSON, encoding="utf-8") as f:
            seeds = json.load(f)
    except ValueError as e:
        sys.exit("%s 不是合法 JSON：%s" % (os.path.basename(PAGES_JSON), e))
    if not isinstance(seeds, list) or not seeds:
        sys.exit("%s 顶层必须是非空数组（首次启动会得到空侧栏）" % os.path.basename(PAGES_JSON))

    keys = []
    for index, seed in enumerate(seeds, start=1):
        where = "%s 第 %d 项" % (os.path.basename(PAGES_JSON), index)
        if not isinstance(seed, dict):
            sys.exit(where + "必须是对象")
        page_key = seed.get("pageKey")
        if not isinstance(page_key, str) or not PAGE_KEY_RE.match(page_key):
            sys.exit(where + "的 pageKey 不合法：%r（需匹配 %s）" % (page_key, PAGE_KEY_RE.pattern))
        if page_key in keys:
            sys.exit(where + "的 pageKey 重复：%s" % page_key)
        keys.append(page_key)
        for field in ("title", "datasourceKey"):
            if not str(seed.get(field) or "").strip():
                sys.exit("%s（%s）缺少 %s" % (where, page_key, field))
        listing = seed.get("list")
        if not isinstance(listing, dict) or not isinstance(listing.get("columns"), list) or not listing["columns"]:
            sys.exit("%s（%s）的 list.columns 必须是非空数组" % (where, page_key))
        form = seed.get("form")
        if not isinstance(form, dict) or not isinstance(form.get("fields"), list):
            sys.exit("%s（%s）的 form.fields 必须是数组（只读页面写空数组）" % (where, page_key))
        for group_name, group, allow_empty in (("columns", listing["columns"], False),
                                               ("filters", listing.get("filters") or [], True)):
            if not isinstance(group, list) or (not group and not allow_empty):
                sys.exit("%s（%s）的 list.%s 格式不对" % (where, page_key, group_name))
            seen = set()
            for item in group:
                key = item.get("key") if isinstance(item, dict) else None
                if not key:
                    sys.exit("%s（%s）的 list.%s 里有条目缺少 key" % (where, page_key, group_name))
                if key in seen:
                    sys.exit("%s（%s）的 list.%s 里 key 重复：%s" % (where, page_key, group_name, key))
                seen.add(key)
    return keys


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
    has_dynamic_route = DYNAMIC_ROUTE in routes
    # 动态路由不参与「必须白名单 / 必须有种子菜单」的检查——它是一条参数化壳路由，
    # 合法性由规则 7 的三方一致性单独约束。normalize 会把它的参数段剥掉，必须先摘出来。
    normalized_routes = {normalize(r) for r in routes if r != DYNAMIC_ROUTE}
    dynamic_page_keys = set(seed_pages())
    icons = nav_icon_keys()

    problems = []

    seen_paths = {}
    for path, name, icon, permission in items:
        if path in seen_paths:
            problems.append("种子菜单路径重复：%s（%s 与 %s）" % (path, seen_paths[path], name))
        seen_paths[path] = name
        if DYNAMIC_PATH_RE.match(path):
            page_key = path[len("/p/"):]
            if not has_dynamic_route:
                problems.append("动态菜单 %s（%s）挂了 /p/ 路径，但 App.tsx 里没有 %s 这条动态路由"
                                % (path, name, DYNAMIC_ROUTE))
            if page_key not in dynamic_page_keys:
                problems.append("动态菜单 %s（%s）的 pageKey 在 pages.json 里没有对应配置"
                                % (path, name))
        else:
            if path not in whitelist:
                problems.append("种子菜单 %s（%s）不在 MenuRoutes 白名单里" % (path, name))
            if path not in normalized_routes:
                problems.append("种子菜单 %s（%s）在 App.tsx 里没有对应 Route" % (path, name))
        if icon not in icons:
            problems.append("种子菜单 %s 的图标 key「%s」不在 NAV_ICONS 里" % (path, icon))
        if permission is not None and permission not in keys:
            problems.append("种子菜单 %s 引用了未登记的权限点：%s" % (path, permission))

    # pages.json 里的 pageKey 也要反向核对：有配置没有菜单，动态页面就没有入口。
    for page_key in sorted(dynamic_page_keys):
        if "/p/" + page_key not in seen_paths:
            problems.append("页面配置 %s 没有对应的 /p/ 种子菜单（首次启动时侧栏没有入口）" % page_key)

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

    print("菜单检查通过：白名单 %d 个页面、种子菜单 %d 条、动态页面 %d 个、权限点 %d 个、图标 key %d 个"
          % (len(whitelist), len(items), len(dynamic_page_keys), len(keys), len(icons)))
    return 0


if __name__ == "__main__":
    sys.exit(main())
