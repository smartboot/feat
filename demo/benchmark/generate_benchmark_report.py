#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
生成基准测试报告的主脚本

此脚本调用parse_benchmark.py：

1. 解析wrk测试结果
2. 解析各框架构建耗时
3. 生成HTML性能对比报告
"""

import os
import sys

from parse_benchmark import (
    collect_results,
    parse_build_times,
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
    # 收集HTTP测试结果
    # ========================================================

    print(
        "收集HTTP性能测试结果..."
    )

    results = collect_results()

    if not results:

        print(
            "错误: 没有找到有效的HTTP测试结果"
        )

        sys.exit(1)

    # ========================================================
    # 收集构建耗时
    # ========================================================

    print(
        "收集框架构建耗时..."
    )

    build_times = parse_build_times()

    if build_times:

        print(
            "构建耗时:"
        )

        for framework, milliseconds in (
            build_times.items()
        ):

            if framework == 'quarkus_prepare':

                print(
                    f"  Quarkus 预构建包准备: "
                    f"{milliseconds / 1000:.2f}s"
                )

            else:

                print(
                    f"  {framework}: "
                    f"{milliseconds / 1000:.2f}s"
                )

    else:

        print(
            "警告: 没有找到构建耗时数据"
        )

    # ========================================================
    # 生成HTML报告
    # ========================================================

    print(
        "生成HTML报告..."
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