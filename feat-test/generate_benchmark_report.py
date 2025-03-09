#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
生成基准测试报告的主脚本

此脚本调用parse_benchmark.py解析Apache Benchmark测试结果，
并生成性能对比报告。
"""

import os
import sys
from parse_benchmark import collect_results, generate_html_report

def main():
    print("开始生成基准测试报告...")
    
    # 确保结果目录存在
    results_dir = 'target/ab-results'
    if not os.path.exists(results_dir):
        os.makedirs(results_dir, exist_ok=True)
        print(f"创建结果目录: {results_dir}")
    
    # 收集测试结果
    print("收集测试结果...")
    results = collect_results()
    
    if not results:
        print("错误: 没有找到有效的测试结果")
        sys.exit(1)
    
    # 生成HTML报告
    print("生成HTML报告...")
    report_file = generate_html_report(results)
    
    print(f"报告生成完成: {report_file}")
    return 0

if __name__ == "__main__":
    sys.exit(main())