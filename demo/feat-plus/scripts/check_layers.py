#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""分层依赖检查：目录结构只有配了检查才算架构，否则只是标签。

规则（白名单，未列出即违规）：
    model      -> 不允许依赖本项目任何包（纯表结构）
    mapper     -> model
    cache      -> 不允许依赖本项目任何包（基础设施，只有 redisun）
    security   -> model, mapper, token, common, cache
    dto        -> model, security, common
    service    -> dto, model, mapper, security, token, common, cache
    controller -> dto, service, security, token, common, cache, seed
    common     -> 不允许
    token      -> 不允许
    seed       -> 不限制（演示设施，接入真实项目时整包删除）

控制器不允许直接摸 mapper：绕过 Service 就等于绕过数据范围校验，
而这种写法编译能过、跑起来只是多看到几条数据，不会报错。
"""
import os
import re
import sys

BASE = "tech.smartboot.feat.demo.plus"
ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                    "..", "src", "main", "java", *BASE.split("."))

ALLOWED = {
    "model": set(),
    "mapper": {"model"},
    "cache": set(),
    "security": {"model", "mapper", "token", "common", "cache"},
    "dto": {"model", "security", "common"},
    "service": {"dto", "model", "mapper", "security", "token", "common", "cache"},
    "controller": {"dto", "service", "security", "token", "common", "cache", "seed"},
    "common": set(),
    "token": set(),
    "seed": None,
}

IMPORT = re.compile(r"^import\s+" + re.escape(BASE) + r"\.(\w+)\.")


def main():
    if not os.path.isdir(ROOT):
        sys.exit("未找到源码目录：%s" % os.path.abspath(ROOT))

    violations = []
    counted = 0
    for layer in sorted(ALLOWED):
        pkg_dir = os.path.join(ROOT, layer)
        if not os.path.isdir(pkg_dir):
            violations.append((layer, "-", "包不存在", "层 %s 缺失" % layer))
            continue
        for name in sorted(os.listdir(pkg_dir)):
            if not name.endswith(".java"):
                continue
            counted += 1
            with open(os.path.join(pkg_dir, name), encoding="utf-8") as f:
                for line in f:
                    m = IMPORT.match(line)
                    if not m:
                        continue
                    target = m.group(1)
                    if target == layer:
                        continue
                    allow = ALLOWED[layer]
                    if allow is None or target in allow:
                        continue
                    violations.append((layer, name, target, "不允许的跨层依赖"))

    # 顶层散落的类（Bootstrap 除外）说明有人绕过了分层，往根包里塞东西
    for name in sorted(os.listdir(ROOT)):
        if name.endswith(".java") and name != "Bootstrap.java":
            violations.append(("<root>", name, "-", "业务类不应直接放在根包"))

    if violations:
        print("违反分层约束 %d 处：" % len(violations))
        for layer, name, target, why in violations:
            print("  %-11s %-32s -> %-11s %s" % (layer, name, target, why))
        return 1

    print("分层检查通过：%d 个类，依赖方向全部符合白名单" % counted)
    return 0


if __name__ == "__main__":
    sys.exit(main())
