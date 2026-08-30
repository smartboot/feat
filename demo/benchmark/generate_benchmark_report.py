#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
生成基准测试报告的主脚本。

此脚本：

1. 解析 wrk 测试结果
2. 解析 Maven 第一次构建耗时
3. 解析 Maven 第二次构建耗时
4. 计算首次构建额外开销
5. 生成 HTML 性能对比报告
"""

import os
import sys

from parse_benchmark import (
    collect_results,
    parse_build_times,
    normalize_build_times,
    generate_html_report
)


def main():

    print(
        "开始生成基准测试报告..."
    )

    # ========================================================
    # 确保结果目录存在
    # ========================================================

    results_dir = 'target/wrk-results'

    if not os.path.exists(results_dir):

        os.makedirs(
            results_dir,
            exist_ok=True
        )

        print(
            f"创建结果目录: {results_dir}"
        )


    # ========================================================
    # 收集 HTTP 测试结果
    # ========================================================

    print(
        "收集 HTTP 性能测试结果..."
    )

    results = collect_results()

    if not results:

        print(
            "错误: 没有找到有效的 HTTP 测试结果"
        )

        sys.exit(1)


    # ========================================================
    # 收集 Maven 构建耗时
    # ========================================================

    print(
        "收集 Maven 构建耗时..."
    )

    build_times = parse_build_times()

    build_data = normalize_build_times(
        build_times
    )


    if build_data:

        print("")
        print(
            "Maven 构建耗时:"
        )

        for framework, data in build_data.items():

            print(
                f"  {framework}:"
            )

            print(
                f"    first : "
                f"{data['first'] / 1000:.2f}s"
            )

            print(
                f"    second: "
                f"{data['second'] / 1000:.2f}s"
            )

            print(
                f"    extra : "
                f"{data['overhead'] / 1000:.2f}s"
            )

    else:

        print(
            "警告: 没有找到 Maven 构建耗时数据"
        )


    # ========================================================
    # 生成 HTML 报告
    # ========================================================

    print(
        "生成 HTML 报告..."
    )

    report_file = generate_html_report(
        results,
        build_times
    )

    print(
        f"报告生成完成: {report_file}"
    )

    return 0


if __name__ == "__main__":
    sys.exit(main())