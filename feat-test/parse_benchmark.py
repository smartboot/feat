#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
解析Apache Benchmark测试结果并生成性能对比报告

此脚本从feat-test/target/ab-results目录读取Apache Benchmark测试结果文件，
提取关键性能指标，并生成包含图表的HTML报告，用于比较Feat、Vert.x和Spring Boot框架的性能。
"""

import os
import re
import sys
from datetime import datetime

# 测试结果目录
RESULTS_DIR = 'target/ab-results'
# 输出报告文件
OUTPUT_FILE = 'target/benchmark-report/index.html'

# 框架名称映射
FRAMEWORK_NAMES = {
    'feat': 'Feat',
    'vertx': 'Vert.x',
    'springboot': 'Spring Boot'
}

# 测试类型映射
TEST_TYPES = {
    'hello': 'Hello World',
    'json': 'JSON响应'
}

# 颜色配置
COLORS = {
    'feat': 'rgba(54, 162, 235, 0.8)',
    'vertx': 'rgba(255, 99, 132, 0.8)',
    'springboot': 'rgba(75, 192, 192, 0.8)'
}


def parse_ab_result(file_path):
    """解析Apache Benchmark结果文件，提取关键性能指标"""
    try:
        with open(file_path, 'r') as f:
            content = f.read()
        
        # 提取关键指标
        metrics = {}
        
        # 每秒请求数 (Requests per second)
        rps_match = re.search(r'Requests per second:\s+([\d\.]+)', content)
        if rps_match:
            metrics['requests_per_second'] = float(rps_match.group(1))
        
        # 平均响应时间 (Time per request - mean across all concurrent requests)
        tpr_match = re.search(r'Time per request:\s+([\d\.]+).*\[ms\].*concurrent', content)
        if tpr_match:
            metrics['time_per_request'] = float(tpr_match.group(1))
        
        # 传输速率 (Transfer rate)
        tr_match = re.search(r'Transfer rate:\s+([\d\.]+)', content)
        if tr_match:
            metrics['transfer_rate'] = float(tr_match.group(1))
        
        # 完成请求数 (Complete requests)
        cr_match = re.search(r'Complete requests:\s+(\d+)', content)
        if cr_match:
            metrics['complete_requests'] = int(cr_match.group(1))
        
        # 失败请求数 (Failed requests)
        fr_match = re.search(r'Failed requests:\s+(\d+)', content)
        if fr_match:
            metrics['failed_requests'] = int(fr_match.group(1))
        else:
            metrics['failed_requests'] = 0
        
        # 计算错误率
        if 'complete_requests' in metrics and metrics['complete_requests'] > 0:
            metrics['error_rate'] = (metrics['failed_requests'] / metrics['complete_requests']) * 100
        else:
            metrics['error_rate'] = 0
        
        return metrics
    except Exception as e:
        print(f"解析文件 {file_path} 时出错: {e}")
        return None


def collect_results():
    """收集所有测试结果"""
    results = {}
    
    # 确保结果目录存在
    if not os.path.exists(RESULTS_DIR):
        print(f"错误: 结果目录 {RESULTS_DIR} 不存在")
        return results
    
    # 遍历结果文件
    for filename in os.listdir(RESULTS_DIR):
        if not filename.endswith('.txt'):
            continue
        
        file_path = os.path.join(RESULTS_DIR, filename)
        
        # 从文件名解析框架和测试类型
        # 例如: feat-hello.txt, vertx-json.txt
        parts = filename.replace('.txt', '').split('-')
        if len(parts) != 2:
            continue
        
        framework, test_type = parts
        
        if framework not in FRAMEWORK_NAMES or test_type not in TEST_TYPES:
            continue
        
        # 解析结果
        metrics = parse_ab_result(file_path)
        if metrics:
            if test_type not in results:
                results[test_type] = {}
            results[test_type][framework] = metrics
    
    return results


def generate_html_report(results):
    """生成HTML报告"""
    # 确保输出目录存在
    os.makedirs(os.path.dirname(OUTPUT_FILE), exist_ok=True)
    
    # 准备图表数据
    chart_data = {}
    for test_type, frameworks in results.items():
        if test_type not in chart_data:
            chart_data[test_type] = {
                'labels': [],
                'rps': [],
                'latency': [],
                'error_rate': [],
                'colors': []
            }
        
        for framework, metrics in sorted(frameworks.items()):
            chart_data[test_type]['labels'].append(FRAMEWORK_NAMES[framework])
            chart_data[test_type]['rps'].append(metrics.get('requests_per_second', 0))
            chart_data[test_type]['latency'].append(metrics.get('time_per_request', 0))
            chart_data[test_type]['error_rate'].append(metrics.get('error_rate', 0))
            chart_data[test_type]['colors'].append(COLORS[framework])
    
    # 生成HTML
    html = f"""
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>框架性能基准测试报告</title>
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
    <style>
        body {{ font-family: 'Arial', sans-serif; margin: 0; padding: 20px; background-color: #f5f5f5; }}
        .container {{ max-width: 1200px; margin: 0 auto; background-color: white; padding: 20px; border-radius: 8px; box-shadow: 0 0 10px rgba(0,0,0,0.1); }}
        h1, h2, h3 {{ color: #333; }}
        .header {{ text-align: center; margin-bottom: 30px; padding-bottom: 20px; border-bottom: 1px solid #eee; }}
        .chart-container {{ display: flex; flex-wrap: wrap; justify-content: space-between; margin-bottom: 30px; }}
        .chart {{ width: 48%; margin-bottom: 20px; background-color: white; padding: 15px; border-radius: 8px; box-shadow: 0 0 5px rgba(0,0,0,0.05); }}
        .summary {{ margin-top: 30px; padding-top: 20px; border-top: 1px solid #eee; }}
        table {{ width: 100%; border-collapse: collapse; margin: 20px 0; }}
        th, td {{ padding: 12px 15px; text-align: left; border-bottom: 1px solid #ddd; }}
        th {{ background-color: #f8f8f8; }}
        tr:hover {{ background-color: #f1f1f1; }}
        .footer {{ text-align: center; margin-top: 30px; color: #777; font-size: 14px; }}
        @media (max-width: 768px) {{ .chart {{ width: 100%; }} }}
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>框架性能基准测试报告</h1>
            <p>生成时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}</p>
        </div>

        <h2>测试概述</h2>
        <p>本报告比较了三个Java Web框架的性能：Feat、Vert.x和Spring Boot。测试使用Apache Benchmark (ab)工具，针对每个框架的Hello World和JSON响应接口进行了性能测试。</p>
        <p>测试参数：1,000,000个请求，并发数100，启用HTTP Keep-Alive。</p>

        <h2>性能对比图表</h2>
"""

    # 为每种测试类型生成图表
    for test_type, data in chart_data.items():
        html += f"""
        <h3>{TEST_TYPES[test_type]}接口测试</h3>
        <div class="chart-container">
            <div class="chart">
                <canvas id="rps-chart-{test_type}"></canvas>
            </div>
            <div class="chart">
                <canvas id="latency-chart-{test_type}"></canvas>
            </div>
        </div>
"""

    # 添加详细数据表格
    html += """
        <h2>详细测试数据</h2>
        <table>
            <tr>
                <th>测试类型</th>
                <th>框架</th>
                <th>每秒请求数</th>
                <th>平均响应时间 (ms)</th>
                <th>错误率 (%)</th>
            </tr>
"""

    for test_type, frameworks in results.items():
        for framework, metrics in sorted(frameworks.items()):
            html += f"""
            <tr>
                <td>{TEST_TYPES[test_type]}</td>
                <td>{FRAMEWORK_NAMES[framework]}</td>
                <td>{metrics.get('requests_per_second', 0):.2f}</td>
                <td>{metrics.get('time_per_request', 0):.2f}</td>
                <td>{metrics.get('error_rate', 0):.2f}</td>
            </tr>
"""

    html += """
        </table>

        <div class="summary">
            <h2>测试结论</h2>
            <p>根据测试结果，可以得出以下结论：</p>
            <ul>
"""

    # 添加简单的结论
    for test_type, frameworks in results.items():
        # 按每秒请求数排序
        sorted_frameworks = sorted(frameworks.items(), 
                                  key=lambda x: x[1].get('requests_per_second', 0), 
                                  reverse=True)
        if sorted_frameworks:
            best_framework, _ = sorted_frameworks[0]
            html += f"<li>在{TEST_TYPES[test_type]}接口测试中，{FRAMEWORK_NAMES[best_framework]}框架表现最佳，每秒处理请求数最多。</li>\n"

    html += """
            </ul>
        </div>

        <div class="footer">
            <p>此报告由自动化基准测试工作流生成</p>
        </div>
    </div>

    <script>
"""

    # 添加Chart.js脚本
    for test_type, data in chart_data.items():
        html += f"""
        // {TEST_TYPES[test_type]} RPS Chart
        new Chart(document.getElementById('rps-chart-{test_type}'), {{
            type: 'bar',
            data: {{
                labels: {data['labels']},
                datasets: [{{
                    label: '每秒请求数',
                    data: {data['rps']},
                    backgroundColor: {data['colors']},
                    borderColor: {data['colors']},
                    borderWidth: 1
                }}]
            }},
            options: {{
                responsive: true,
                plugins: {{
                    title: {{
                        display: true,
                        text: '每秒请求数 (RPS) - 越高越好'
                    }},
                    legend: {{
                        display: false
                    }}
                }},
                scales: {{
                    y: {{
                        beginAtZero: true
                    }}
                }}
            }}
        }});

        // {TEST_TYPES[test_type]} Latency Chart
        new Chart(document.getElementById('latency-chart-{test_type}'), {{
            type: 'bar',
            data: {{
                labels: {data['labels']},
                datasets: [{{
                    label: '平均响应时间 (ms)',
                    data: {data['latency']},
                    backgroundColor: {data['colors']},
                    borderColor: {data['colors']},
                    borderWidth: 1
                }}]
            }},
            options: {{
                responsive: true,
                plugins: {{
                    title: {{
                        display: true,
                        text: '平均响应时间 (ms) - 越低越好'
                    }},
                    legend: {{
                        display: false
                    }}
                }},
                scales: {{
                    y: {{
                        beginAtZero: true
                    }}
                }}
            }}
        }});
"""

    html += """
    </script>
</body>
</html>
"""

    # 写入文件
    with open(OUTPUT_FILE, 'w', encoding='utf-8') as f:
        f.write(html)

    print(f"报告已生成: {OUTPUT_FILE}")
    return OUTPUT_FILE