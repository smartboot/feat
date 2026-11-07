#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""事务约束检查：把「写操作必须原子」变成可验证的规则。

为什么需要脚本而不是写在文档里：事务写错了不报错。注入的 Mapper 每个方法自己开会话提交，
业务代码里混用一个，那一笔写就落在事务外，接口照常返回 200，回滚时也不会被收回。
这类问题人能记住、AI 记不住——它写出来的代码语法对、逻辑对、只有事务不对。

三条规则（违反即失败）：

  A. 一个方法里对「注入的 Mapper」写两次及以上，必须整段包在 txTemplate.call / txTemplate.run 里。
     单条语句本身就是原子的，不强制，避免为了统一把每个 update 都包一层。

  B. 事务 lambda 体内不许出现注入的 Mapper 字段。
     事务体内一律用 session.getMapper(...)：两套 Mapper 混用等于把后半截写扔出事务。

  C. 方法里已经有事务块时，注入 Mapper 的写必须全部落在事务块内。
     「一半进事务、一半靠自动提交」是最难发现的一种错法：回滚时只回滚一半。

seed 包不检查：它用 TRUNCATE 重置演示数据，DDL 会隐式提交，包事务没有意义。
接收 Mapper 参数的方法（如 replaceRoles(UserMapper users, ...)）视为事务片段，不单独判 A——
它自己不开事务，由调用方决定边界。

局限（有意接受）：这是文本级检查，认不出「先把注入 Mapper 存进局部变量再写」这种绕法。
它拦的是最常见、最容易被 AI 写出来的那种错法，不是形式化证明。
"""
import os
import re
import sys

BASE = "tech.smartboot.feat.demo.plus"
ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                    "..", "src", "main", "java", *BASE.split("."))

# 写操作：Mapper 里以下列前缀命名的方法。select / count 是读，不在内。
WRITE_CALL = re.compile(r"(\w+)\s*\.\s*(insert|update|softDelete|restore|truncate|delete|increase|rename)\w*\s*\(")
TX_OPEN = re.compile(r"\b\w+\s*\.\s*(call|run)\s*\(")
# @Autowired 字段：抓到「类型 + 名字」，只把 *Mapper 类型算作写库入口。
AUTOWIRED_FIELD = re.compile(
    r"@Autowired\s*(?:\([^)]*\))?\s*(?:private|protected|public)?\s*([\w.]+)(?:<[^;]*>)?\s+(\w+)\s*;", re.S)
METHOD_HEAD = re.compile(r"(?m)^    (?:public|private|protected)\s[^;{}]*\([^)]*\)\s*(?:throws [\w, .]+)?\{")


def strip_noise(src):
    """把注释与字符串字面量替换成等长空白，保证括号计数不被内容干扰。"""
    out = []
    i, n = 0, len(src)
    while i < n:
        ch = src[i]
        two = src[i:i + 2]
        if two == "//":
            j = src.find("\n", i)
            j = n if j < 0 else j
            out.append(" " * (j - i))
            i = j
        elif two == "/*":
            j = src.find("*/", i + 2)
            j = n if j < 0 else j + 2
            out.append(" " * (j - i))
            i = j
        elif ch in "\"'":
            quote = ch
            j = i + 1
            while j < n:
                if src[j] == "\\":
                    j += 2
                    continue
                if src[j] == quote:
                    j += 1
                    break
                j += 1
            out.append(" " * (j - i))
            i = j
        else:
            out.append(ch)
            i += 1
    return "".join(out)


def match_pair(text, start, open_ch, close_ch):
    """text[start] 必须是 open_ch，返回配对 close_ch 之后的下标。"""
    depth = 0
    for i in range(start, len(text)):
        if text[i] == open_ch:
            depth += 1
        elif text[i] == close_ch:
            depth -= 1
            if depth == 0:
                return i + 1
    return len(text)


def tx_regions(body):
    """返回 body 里所有 tx 调用括号内的区间 [start, end)。"""
    regions = []
    for m in TX_OPEN.finditer(body):
        open_idx = body.find("(", m.end() - 1)
        regions.append((open_idx, match_pair(body, open_idx, "(", ")")))
    return regions


def main():
    if not os.path.isdir(ROOT):
        sys.exit("未找到源码目录：%s" % os.path.abspath(ROOT))

    violations = []
    counted = 0
    pkg_dir = os.path.join(ROOT, "service")
    for name in sorted(os.listdir(pkg_dir)):
        if not name.endswith(".java"):
            continue
        counted += 1
        with open(os.path.join(pkg_dir, name), encoding="utf-8") as f:
            src = strip_noise(f.read())

        injected = set(field for typ, field in AUTOWIRED_FIELD.findall(src) if typ.endswith("Mapper"))

        for head in METHOD_HEAD.finditer(src):
            body_start = head.end() - 1
            body_end = match_pair(src, body_start, "{", "}")
            body = src[body_start:body_end]
            method = head.group(0).strip().split("(")[0].split()[-1]

            regions = tx_regions(body)
            writes = [w for w in WRITE_CALL.finditer(body) if w.group(1) in injected]

            if len(writes) >= 2 and not regions:
                violations.append((name, method, "%d 处写库没有包在事务里" % len(writes)))

            if regions and writes:
                covered = [any(s <= w.start() < e for s, e in regions) for w in writes]
                if not all(covered):
                    violations.append((name, method, "同一方法里一部分写在事务内、一部分自动提交"))

            for start, end in regions:
                inner = body[start:end]
                for field in injected:
                    if re.search(r"\b%s\s*\." % re.escape(field), inner):
                        violations.append((name, method,
                                           "事务体内用了注入的 %s，应改用 session.getMapper(...)" % field))

    if violations:
        print("违反事务约束 %d 处：" % len(violations))
        for name, method, why in violations:
            print("  %-28s %-24s %s" % (name, method, why))
        return 1

    print("事务检查通过：%d 个 Service 类，多语句写全部包在模板内，事务体未混用注入 Mapper" % counted)
    return 0


if __name__ == "__main__":
    sys.exit(main())
