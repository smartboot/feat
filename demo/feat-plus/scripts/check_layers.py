#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""分层依赖检查：目录结构只有配了检查才算架构，否则只是标签。

规则（白名单，未列出即违规）。`dw` 是动态页面模块，自带一套同构的分层：
    model       -> 不允许依赖本项目任何包（纯表结构）
    mapper      -> model
    cache       -> 不允许依赖本项目任何包（基础设施，只有 redisun）
    tx          -> 不允许依赖本项目任何包（基础设施，只有 mybatis）
    audit       -> model, mapper, tx, common
    security    -> model, mapper, token, common, cache
    dto         -> model, security, common
    service     -> dto, model, mapper, security, token, common, cache, tx, audit, dw
    controller  -> dto, service, security, token, common, cache, seed
    common      -> 不允许
    token       -> 不允许
    seed        -> 不限制（演示设施，接入真实项目时整包删除）

    dw/model      -> 不允许
    dw/mapper     -> dw.model
    dw/dto        -> dw.model, common
    dw/service    -> dw.dto, dw.mapper, dw.model, audit, cache, common, security, tx
    dw/controller -> dw.dto, dw.service, common, security
    dw/seed       -> dw.mapper, dw.model

三条硬规则的由来：

1. 控制器不允许直接摸 mapper：绕过 Service 就等于绕过数据范围校验，
   而这种写法编译能过、跑起来只是多看到几条数据，不会报错。
2. `dw` 内部只允许依赖基础设施（audit / cache / common / security / tx），
   不允许出现 `dw.* -> service`：平台层一旦反向依赖业务层，这个模块就不能整体抽走，
   「统一包」也就只剩目录好看。
3. 业务层可以用 `dw`（实现 PageDataSource、菜单校验页面存在性），反过来不行。
"""
import os
import re
import sys

BASE = "tech.smartboot.feat.demo.plus"
ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                    "..", "src", "main", "java", *BASE.split("."))

# 键是用 "/" 分隔的相对目录；值里用 "." 分隔的模块名（"dw" 表示整个 dw 模块）。
ALLOWED = {
    "model": set(),
    "mapper": {"model"},
    "cache": set(),
    "tx": set(),
    "audit": {"model", "mapper", "tx", "common"},
    "security": {"model", "mapper", "token", "common", "cache"},
    "dto": {"model", "security", "common"},
    "service": {"dto", "model", "mapper", "security", "token", "common", "cache", "tx",
                "audit", "dw"},
    "controller": {"dto", "service", "security", "token", "common", "cache", "seed"},
    "common": set(),
    "token": set(),
    "seed": None,

    "dw/model": set(),
    "dw/mapper": {"dw.model"},
    "dw/dto": {"dw.model", "common"},
    "dw/service": {"dw.dto", "dw.mapper", "dw.model", "audit", "cache", "common",
                   "security", "tx"},
    "dw/controller": {"dw.dto", "dw.service", "common", "security"},
    "dw/seed": {"dw.mapper", "dw.model"},
}

# 顶层包必须是这里声明过的，否则新包会绕过全部检查（检查器只认这一份清单）。
TOP_PACKAGES = {"audit", "cache", "common", "controller", "dto", "dw", "mapper",
                "model", "security", "seed", "service", "token", "tx"}

IMPORT = re.compile(r"^import\s+" + re.escape(BASE) + r"\.(\w+)(?:\.(\w+))?\.")


def target_of(seg1, seg2):
    """把 import 的前两段变成一个可比较的模块名。"""
    if seg1 == "dw":
        return "dw." + seg2 if seg2 else "dw"
    return seg1


def allowed_by(allow, target):
    """allow 里的条目既可以是整模块（dw），也可以是具体子层（dw.dto）。"""
    for item in allow:
        if target == item or target.startswith(item + "."):
            return True
    return False


def main():
    if not os.path.isdir(ROOT):
        sys.exit("未找到源码目录：%s" % os.path.abspath(ROOT))

    violations = []
    counted = 0
    for key in sorted(ALLOWED):
        pkg_dir = os.path.join(ROOT, *key.split("/"))
        if not os.path.isdir(pkg_dir):
            violations.append((key, "-", "包不存在", "层 %s 缺失" % key))
            continue
        own = key.replace("/", ".")
        for name in sorted(os.listdir(pkg_dir)):
            if not name.endswith(".java"):
                continue
            counted += 1
            with open(os.path.join(pkg_dir, name), encoding="utf-8") as f:
                for line in f:
                    m = IMPORT.match(line)
                    if not m:
                        continue
                    target = target_of(m.group(1), m.group(2))
                    if target == own or target.startswith(own + "."):
                        continue
                    allow = ALLOWED[key]
                    if allow is None or allowed_by(allow, target):
                        continue
                    violations.append((key, name, target, "不允许的跨层依赖"))

    # 顶层散落的类（Bootstrap 除外）说明有人绕过了分层，往根包里塞东西
    for name in sorted(os.listdir(ROOT)):
        if name.endswith(".java") and name != "Bootstrap.java":
            violations.append(("<root>", name, "-", "业务类不应直接放在根包"))

    # 未声明的顶层包：不报出来就会静默零检查
    for name in sorted(os.listdir(ROOT)):
        if os.path.isdir(os.path.join(ROOT, name)) and name not in TOP_PACKAGES:
            violations.append((name, "-", "-", "未声明的顶层包，先在本脚本里声明依赖规则"))

    if violations:
        print("违反分层约束 %d 处：" % len(violations))
        for layer, name, target, why in violations:
            print("  %-14s %-34s -> %-12s %s" % (layer, name, target, why))
        return 1

    print("分层检查通过：%d 个类，依赖方向全部符合白名单" % counted)
    return 0


if __name__ == "__main__":
    sys.exit(main())
