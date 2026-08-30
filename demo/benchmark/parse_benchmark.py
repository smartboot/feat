#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
解析 wrk 测试结果并生成性能对比报告。

此脚本从 target/wrk-results 目录读取：

1. wrk 性能测试结果
2. Maven 第一次构建耗时
3. Maven 第二次构建耗时

Maven 构建测试：

first
    全新的 Maven Local Repository。
    包含依赖下载、插件下载、编译、打包。

second
    复用第一次构建产生的 Maven Local Repository。
    主要衡量依赖已经缓存后的构建效率。

最终生成包含：

1. HTTP 性能对比
2. Maven 第一次构建耗时
3. Maven 第二次构建耗时
4. Maven 首次构建额外开销
5. 详细测试数据
6. 测试结论

的 HTML 报告。
"""

import os
import re
from datetime import datetime


# ============================================================
# 路径配置
# ============================================================

RESULTS_DIR = 'target/wrk-results'

BUILD_TIMES_FILE = os.path.join(
    RESULTS_DIR,
    'build-times.txt'
)

OUTPUT_FILE = 'target/benchmark-report/index.html'


# ============================================================
# 框架名称映射
# ============================================================

FRAMEWORK_NAMES = {
    'feat': 'Feat',
    'vertx': 'Vert.x',
    'springboot': 'Spring Boot',
    'quarkus': 'Quarkus'
}


BUILD_FRAMEWORK_NAMES = {
    'feat': 'Feat',
    'vertx': 'Vert.x',
    'springboot': 'Spring Boot'
}


# ============================================================
# 测试类型
# ============================================================

TEST_TYPES = {
    'hello': 'Hello World',
    'json': 'JSON响应'
}


# ============================================================
# 颜色
# ============================================================

COLORS = {
    'feat': 'rgba(54, 162, 235, 0.8)',
    'vertx': 'rgba(255, 99, 132, 0.8)',
    'springboot': 'rgba(75, 192, 192, 0.8)',
    'quarkus': 'rgba(153, 102, 255, 0.8)'
}


# ============================================================
# wrk 数据解析
# ============================================================

def parse_size_to_kb(value, unit):
    """将 wrk 传输速率单位转换为 KB/s。"""

    multipliers = {
        'B': 1 / 1024,
        'KB': 1,
        'MB': 1024,
        'GB': 1024 * 1024
    }

    return float(value) * multipliers.get(unit, 1)


def parse_time_to_ms(value, unit):
    """将 wrk 延迟单位转换为毫秒。"""

    multipliers = {
        'us': 0.001,
        'ms': 1,
        's': 1000
    }

    return float(value) * multipliers.get(unit, 1)


def parse_wrk_result(file_path):
    """解析 wrk 结果文件，提取关键性能指标。"""

    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()

        metrics = {}

        # --------------------------------------------------------
        # 每秒请求数
        # --------------------------------------------------------

        rps_match = re.search(
            r'Requests/sec:\s+([\d\.]+)',
            content
        )

        if rps_match:
            metrics['requests_per_second'] = float(
                rps_match.group(1)
            )

        # --------------------------------------------------------
        # 平均响应时间
        # --------------------------------------------------------

        latency_match = re.search(
            r'Latency\s+([\d\.]+)(us|ms|s)',
            content
        )

        if latency_match:
            metrics['time_per_request'] = parse_time_to_ms(
                latency_match.group(1),
                latency_match.group(2)
            )

        # --------------------------------------------------------
        # 传输速率
        # --------------------------------------------------------

        tr_match = re.search(
            r'Transfer/sec:\s+([\d\.]+)(B|KB|MB|GB)',
            content
        )

        if tr_match:
            metrics['transfer_rate'] = parse_size_to_kb(
                tr_match.group(1),
                tr_match.group(2)
            )

        # --------------------------------------------------------
        # 完成请求数
        # --------------------------------------------------------

        cr_match = re.search(
            r'(\d+)\s+requests in\s+[\d\.]+[smh]',
            content
        )

        if cr_match:
            metrics['complete_requests'] = int(
                cr_match.group(1)
            )

        # --------------------------------------------------------
        # 错误请求
        # --------------------------------------------------------

        failed_requests = 0

        socket_errors_match = re.search(
            r'Socket errors:\s+connect\s+(\d+),\s+read\s+(\d+),\s+write\s+(\d+),\s+timeout\s+(\d+)',
            content
        )

        if socket_errors_match:
            failed_requests += sum(
                int(value)
                for value in socket_errors_match.groups()
            )

        non_success_match = re.search(
            r'Non-2xx or 3xx responses:\s+(\d+)',
            content
        )

        if non_success_match:
            failed_requests += int(
                non_success_match.group(1)
            )

        metrics['failed_requests'] = failed_requests

        # --------------------------------------------------------
        # 错误率
        # --------------------------------------------------------

        if (
            'complete_requests' in metrics
            and metrics['complete_requests'] > 0
        ):
            metrics['error_rate'] = (
                metrics['failed_requests']
                / metrics['complete_requests']
            ) * 100
        else:
            metrics['error_rate'] = 0

        return metrics

    except Exception as e:
        print(
            f"解析文件 {file_path} 时出错: {e}"
        )

        return None


# ============================================================
# 收集 wrk 测试结果
# ============================================================

def collect_results():
    """收集所有 wrk 测试结果。"""

    results = {}

    if not os.path.exists(RESULTS_DIR):
        print(
            f"错误: 结果目录 {RESULTS_DIR} 不存在"
        )

        return results

    for filename in os.listdir(RESULTS_DIR):

        if not filename.endswith('.txt'):
            continue

        if filename == 'build-times.txt':
            continue

        file_path = os.path.join(
            RESULTS_DIR,
            filename
        )

        parts = filename.replace(
            '.txt',
            ''
        ).split('-')

        if len(parts) != 2:
            continue

        framework, test_type = parts

        if (
            framework not in FRAMEWORK_NAMES
            or test_type not in TEST_TYPES
        ):
            continue

        metrics = parse_wrk_result(
            file_path
        )

        if metrics:
            if test_type not in results:
                results[test_type] = {}

            results[test_type][framework] = metrics

    return results


# ============================================================
# 解析 Maven 构建耗时
# ============================================================

def parse_build_times():
    """
    解析 Maven 构建耗时。

    格式：

    feat_first=12345
    feat_second=2345

    vertx_first=23456
    vertx_second=3456

    springboot_first=34567
    springboot_second=4567
    """

    build_times = {}

    if not os.path.exists(BUILD_TIMES_FILE):
        print(
            f"警告: 构建耗时文件不存在: "
            f"{BUILD_TIMES_FILE}"
        )

        return build_times

    try:
        with open(
            BUILD_TIMES_FILE,
            'r',
            encoding='utf-8'
        ) as f:

            for line in f:

                line = line.strip()

                if (
                    not line
                    or line.startswith('#')
                    or '=' not in line
                ):
                    continue

                key, value = line.split(
                    '=',
                    1
                )

                key = key.strip()
                value = value.strip()

                try:
                    build_times[key] = float(value)

                except ValueError:
                    print(
                        f"警告: 无法解析构建耗时: {line}"
                    )

    except Exception as e:
        print(
            f"读取构建耗时文件失败: {e}"
        )

    return build_times


# ============================================================
# 构建耗时整理
# ============================================================

def normalize_build_times(build_times):
    """
    将：

        feat_first
        feat_second

    整理成：

        {
            'feat': {
                'first': xxx,
                'second': xxx,
                'overhead': xxx
            }
        }
    """

    result = {}

    for framework in BUILD_FRAMEWORK_NAMES:

        first_key = f'{framework}_first'
        second_key = f'{framework}_second'

        if (
            first_key not in build_times
            and second_key not in build_times
        ):
            continue

        first = build_times.get(
            first_key,
            0
        )

        second = build_times.get(
            second_key,
            0
        )

        overhead = max(
            0,
            first - second
        )

        result[framework] = {
            'first': first,
            'second': second,
            'overhead': overhead
        }

    return result


# ============================================================
# HTML 工具
# ============================================================

def js_array(values):
    """将 Python 列表转换为简单 JS 数组。"""

    return str(values).replace(
        "'",
        '"'
    )


# ============================================================
# HTML 报告
# ============================================================

def generate_html_report(
    results,
    build_times=None
):
    """生成 HTML 性能报告。"""

    if build_times is None:
        build_times = parse_build_times()

    build_data = normalize_build_times(
        build_times
    )

    os.makedirs(
        os.path.dirname(OUTPUT_FILE),
        exist_ok=True
    )

    # ========================================================
    # HTTP 图表数据
    # ========================================================

    chart_data = {}

    for test_type, frameworks in results.items():

        chart_data[test_type] = {
            'labels': [],
            'rps': [],
            'latency': [],
            'error_rate': [],
            'colors': []
        }

        for framework, metrics in sorted(
            frameworks.items()
        ):

            chart_data[test_type]['labels'].append(
                FRAMEWORK_NAMES[framework]
            )

            chart_data[test_type]['rps'].append(
                metrics.get(
                    'requests_per_second',
                    0
                )
            )

            chart_data[test_type]['latency'].append(
                metrics.get(
                    'time_per_request',
                    0
                )
            )

            chart_data[test_type]['error_rate'].append(
                metrics.get(
                    'error_rate',
                    0
                )
            )

            chart_data[test_type]['colors'].append(
                COLORS[framework]
            )

    # ========================================================
    # Maven 图表数据
    # ========================================================

    build_labels = []
    first_build_values = []
    second_build_values = []
    overhead_values = []

    for framework in (
        'feat',
        'vertx',
        'springboot'
    ):

        if framework not in build_data:
            continue

        build_labels.append(
            BUILD_FRAMEWORK_NAMES[framework]
        )

        data = build_data[framework]

        first_build_values.append(
            round(
                data['first'] / 1000,
                2
            )
        )

        second_build_values.append(
            round(
                data['second'] / 1000,
                2
            )
        )

        overhead_values.append(
            round(
                data['overhead'] / 1000,
                2
            )
        )

    # ========================================================
    # HTML Header
    # ========================================================

    html = f"""
<!DOCTYPE html>
<html lang="zh-CN">

<head>

    <meta charset="UTF-8">

    <meta
        name="viewport"
        content="width=device-width, initial-scale=1.0"
    >

    <title>框架性能基准测试报告</title>

    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>

    <style>

        body {{
            font-family:
                -apple-system,
                BlinkMacSystemFont,
                "Segoe UI",
                Arial,
                sans-serif;

            margin: 0;
            padding: 20px;

            background-color: #f5f5f5;
        }}

        .container {{
            max-width: 1200px;

            margin: 0 auto;

            background-color: white;

            padding: 20px;

            border-radius: 8px;

            box-shadow:
                0 0 10px rgba(0,0,0,0.1);
        }}

        h1,
        h2,
        h3 {{
            color: #333;
        }}

        .header {{
            text-align: center;

            margin-bottom: 30px;

            padding-bottom: 20px;

            border-bottom:
                1px solid #eee;
        }}

        .chart-container {{
            display: flex;

            flex-wrap: wrap;

            justify-content:
                space-between;

            margin-bottom: 30px;
        }}

        .chart {{
            width: 48%;

            margin-bottom: 20px;

            background-color: white;

            padding: 15px;

            border-radius: 8px;

            box-shadow:
                0 0 5px rgba(0,0,0,0.05);
        }}

        .chart-full {{
            width: 100%;

            margin-bottom: 20px;

            background-color: white;

            padding: 15px;

            border-radius: 8px;

            box-shadow:
                0 0 5px rgba(0,0,0,0.05);
        }}

        .summary {{
            margin-top: 30px;

            padding-top: 20px;

            border-top:
                1px solid #eee;
        }}

        table {{
            width: 100%;

            border-collapse:
                collapse;

            margin: 20px 0;
        }}

        th,
        td {{
            padding: 12px 15px;

            text-align: left;

            border-bottom:
                1px solid #ddd;
        }}

        th {{
            background-color: #f8f8f8;
        }}

        tr:hover {{
            background-color: #f1f1f1;
        }}

        .highlight {{
            font-weight: bold;
        }}

        .muted {{
            color: #777;
        }}

        .footer {{
            text-align: center;

            margin-top: 30px;

            color: #777;

            font-size: 14px;
        }}

        .info {{
            background: #f8f9fa;

            border-left:
                4px solid #999;

            padding: 12px 16px;

            margin: 15px 0;
        }}

        .metric-card {{
            display: flex;

            flex-wrap: wrap;

            gap: 15px;

            margin: 20px 0;
        }}

        .metric {{
            flex: 1;

            min-width: 200px;

            padding: 18px;

            background: #f8f8f8;

            border-radius: 8px;
        }}

        .metric-title {{
            font-size: 14px;

            color: #777;

            margin-bottom: 8px;
        }}

        .metric-value {{
            font-size: 26px;

            font-weight: bold;

            color: #333;
        }}

        @media (max-width: 768px) {{

            .chart,
            .chart-full {{
                width: 100%;
            }}

        }}

    </style>

</head>

<body>

<div class="container">

    <div class="header">

        <h1>
            框架性能基准测试报告
        </h1>

        <p>
            生成时间:
            {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}
        </p>

    </div>


    <h2>测试概述</h2>

    <p>
        本报告比较了四个 Java Web 框架：
        Feat、Vert.x、Quarkus 和 Spring Boot。
    </p>

    <p>
        HTTP 性能测试使用 wrk，
        针对每个框架的 Hello World
        和 JSON 响应接口进行测试。
    </p>

    <p>
        HTTP 测试参数：
        4 个线程，100 个连接，
        持续 60 秒，开启延迟统计。
    </p>


    <div class="info">

        <strong>Maven 构建测试说明：</strong>

        <p>
            第一次构建使用全新的 Maven Local Repository，
            因此包含 Maven 插件、第三方依赖下载以及编译、
            打包等完整开销。
        </p>

        <p>
            第二次构建复用第一次构建产生的 Maven Local Repository，
            主要衡量依赖已经缓存后的日常构建效率。
        </p>

        <p>
            每个框架使用独立 Maven Repository，
            避免框架之间共享依赖缓存。
        </p>

    </div>
"""


    # ========================================================
    # Maven Build
    # ========================================================

    html += """
    <h2>Maven 构建性能</h2>

    <div class="chart-container">

        <div class="chart">
            <canvas id="maven-first-chart"></canvas>
        </div>

        <div class="chart">
            <canvas id="maven-second-chart"></canvas>
        </div>

    </div>

    <div class="chart-full">
        <canvas id="maven-overhead-chart"></canvas>
    </div>


    <h3>Maven 构建耗时明细</h3>

    <table>

        <tr>
            <th>框架</th>
            <th>第一次构建</th>
            <th>第二次构建</th>
            <th>首次额外开销</th>
            <th>缓存后效率</th>
        </tr>
"""


    for framework in (
        'feat',
        'vertx',
        'springboot'
    ):

        if framework not in build_data:
            continue

        data = build_data[framework]

        first = data['first']
        second = data['second']
        overhead = data['overhead']

        if first > 0:
            cache_efficiency = (
                second / first
            ) * 100
        else:
            cache_efficiency = 0

        html += f"""
        <tr>

            <td>
                <strong>
                    {BUILD_FRAMEWORK_NAMES[framework]}
                </strong>
            </td>

            <td>
                {first / 1000:.2f} 秒
                <span class="muted">
                    ({first:.0f} ms)
                </span>
            </td>

            <td>
                {second / 1000:.2f} 秒
                <span class="muted">
                    ({second:.0f} ms)
                </span>
            </td>

            <td>
                {overhead / 1000:.2f} 秒
                <span class="muted">
                    ({overhead:.0f} ms)
                </span>
            </td>

            <td>
                {cache_efficiency:.1f}%
            </td>

        </tr>
"""


    html += """
    </table>

    <div class="info">

        <strong>如何理解 Maven 构建数据：</strong>

        <p>
            <strong>第一次构建</strong>更接近开发者首次
            clone 项目之后的真实体验。
        </p>

        <p>
            <strong>第二次构建</strong>更接近日常开发中
            Maven 依赖已经存在于本地缓存后的体验。
        </p>

        <p>
            <strong>首次额外开销</strong>为第一次构建与第二次构建
            的耗时差值，可用于观察依赖和插件准备所带来的额外成本。
        </p>

    </div>
"""


    # ========================================================
    # Quarkus
    # ========================================================

    html += """
    <div class="info">

        <strong>Quarkus 构建说明：</strong>

        <p>
            Quarkus 当前使用仓库中的预构建
            <code>quarkus-app.zip</code>，
            因此没有纳入 Maven 编译耗时对比。
        </p>

    </div>
"""


    # ========================================================
    # HTTP 性能
    # ========================================================

    html += """
    <h2>HTTP 性能对比</h2>
"""


    for test_type, data in chart_data.items():

        html += f"""
    <h3>
        {TEST_TYPES[test_type]} 接口测试
    </h3>

    <div class="chart-container">

        <div class="chart">

            <canvas
                id="rps-chart-{test_type}">
            </canvas>

        </div>

        <div class="chart">

            <canvas
                id="latency-chart-{test_type}">
            </canvas>

        </div>

    </div>
"""


    # ========================================================
    # HTTP Detail
    # ========================================================

    html += """
    <h2>HTTP 详细测试数据</h2>

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

        for framework, metrics in sorted(
            frameworks.items()
        ):

            html += f"""
        <tr>

            <td>
                {TEST_TYPES[test_type]}
            </td>

            <td>
                {FRAMEWORK_NAMES[framework]}
            </td>

            <td>
                {metrics.get(
                    'requests_per_second',
                    0
                ):.2f}
            </td>

            <td>
                {metrics.get(
                    'time_per_request',
                    0
                ):.2f}
            </td>

            <td>
                {metrics.get(
                    'error_rate',
                    0
                ):.2f}
            </td>

        </tr>
"""


    html += """
    </table>


    <div class="summary">

        <h2>测试结论</h2>

        <p>
            根据测试结果，可以得出以下结论：
        </p>

        <ul>
"""


    # ========================================================
    # HTTP Conclusion
    # ========================================================

    for test_type, frameworks in results.items():

        sorted_frameworks = sorted(
            frameworks.items(),
            key=lambda x:
                x[1].get(
                    'requests_per_second',
                    0
                ),
            reverse=True
        )

        if sorted_frameworks:

            best_framework, best_metrics = (
                sorted_frameworks[0]
            )

            html += f"""
            <li>
                在
                {TEST_TYPES[test_type]}
                接口测试中，
                <strong>
                    {FRAMEWORK_NAMES[best_framework]}
                </strong>
                表现最佳，
                每秒处理
                <strong>
                    {best_metrics.get(
                        'requests_per_second',
                        0
                    ):.2f}
                </strong>
                个请求。
            </li>
"""


    # ========================================================
    # Maven Conclusion
    # ========================================================

    if build_data:

        fastest_first = min(
            build_data,
            key=lambda x:
                build_data[x]['first']
        )

        fastest_second = min(
            build_data,
            key=lambda x:
                build_data[x]['second']
        )

        fastest_first_time = (
            build_data[fastest_first]['first']
            / 1000
        )

        fastest_second_time = (
            build_data[fastest_second]['second']
            / 1000
        )

        html += f"""
            <li>
                在第一次 Maven 构建中，
                <strong>
                    {BUILD_FRAMEWORK_NAMES[fastest_first]}
                </strong>
                耗时最低，
                为
                <strong>
                    {fastest_first_time:.2f}
                </strong>
                秒。
            </li>

            <li>
                在第二次 Maven 构建中，
                <strong>
                    {BUILD_FRAMEWORK_NAMES[fastest_second]}
                </strong>
                耗时最低，
                为
                <strong>
                    {fastest_second_time:.2f}
                </strong>
                秒。
            </li>
"""


        # ----------------------------------------------------
        # Feat vs Other
        # ----------------------------------------------------

        if 'feat' in build_data:

            feat_first = build_data['feat']['first']
            feat_second = build_data['feat']['second']

            for framework in (
                'vertx',
                'springboot'
            ):

                if framework not in build_data:
                    continue

                other_first = build_data[
                    framework
                ]['first']

                other_second = build_data[
                    framework
                ]['second']


                # First build
                if feat_first < other_first:

                    improvement = (
                        (
                            other_first
                            - feat_first
                        )
                        / other_first
                    ) * 100

                    html += f"""
            <li>
                第一次构建中，
                Feat 相比
                {BUILD_FRAMEWORK_NAMES[framework]}
                快
                <strong>
                    {improvement:.2f}%
                </strong>。
            </li>
"""

                # Second build
                if feat_second < other_second:

                    improvement = (
                        (
                            other_second
                            - feat_second
                        )
                        / other_second
                    ) * 100

                    html += f"""
            <li>
                第二次构建中，
                Feat 相比
                {BUILD_FRAMEWORK_NAMES[framework]}
                快
                <strong>
                    {improvement:.2f}%
                </strong>。
            </li>
"""


    html += """
        </ul>

    </div>


    <div class="footer">

        <p>
            此报告由自动化基准测试工作流生成
        </p>

        <p>
            Maven 第一次构建使用独立且全新的 Maven Local Repository，
            第二次构建复用第一次构建产生的缓存。
        </p>

        <p>
            构建耗时为 CI 环境中的实际墙钟时间，
            会受到网络速度、Maven Central 响应速度、
            GitHub Actions Runner 负载等因素影响。
        </p>

    </div>

</div>


<script>
"""


    # ========================================================
    # Maven First Build Chart
    # ========================================================

    html += f"""
    new Chart(
        document.getElementById(
            'maven-first-chart'
        ),
        {{

            type: 'bar',

            data: {{

                labels:
                    {js_array(build_labels)},

                datasets: [{{

                    label:
                        '第一次构建（秒）',

                    data:
                        {js_array(first_build_values)},

                    backgroundColor:
                        'rgba(54, 162, 235, 0.8)',

                    borderWidth: 1

                }}]

            }},

            options: {{

                responsive: true,

                plugins: {{

                    title: {{

                        display: true,

                        text:
                            'Maven 第一次构建 - 越低越好'

                    }},

                    legend: {{

                        display: false

                    }}

                }},

                scales: {{

                    y: {{

                        beginAtZero: true,

                        title: {{

                            display: true,

                            text:
                                '耗时（秒）'

                        }}

                    }}

                }}

            }}

        }}
    );
"""


    # ========================================================
    # Maven Second Build Chart
    # ========================================================

    html += f"""
    new Chart(
        document.getElementById(
            'maven-second-chart'
        ),
        {{

            type: 'bar',

            data: {{

                labels:
                    {js_array(build_labels)},

                datasets: [{{

                    label:
                        '第二次构建（秒）',

                    data:
                        {js_array(second_build_values)},

                    backgroundColor:
                        'rgba(75, 192, 192, 0.8)',

                    borderWidth: 1

                }}]

            }},

            options: {{

                responsive: true,

                plugins: {{

                    title: {{

                        display: true,

                        text:
                            'Maven 第二次构建 - 越低越好'

                    }},

                    legend: {{

                        display: false

                    }}

                }},

                scales: {{

                    y: {{

                        beginAtZero: true,

                        title: {{

                            display: true,

                            text:
                                '耗时（秒）'

                        }}

                    }}

                }}

            }}

        }}
    );
"""


    # ========================================================
    # Maven Download Overhead Chart
    # ========================================================

    html += f"""
    new Chart(
        document.getElementById(
            'maven-overhead-chart'
        ),
        {{

            type: 'bar',

            data: {{

                labels:
                    {js_array(build_labels)},

                datasets: [{{

                    label:
                        '首次额外开销（秒）',

                    data:
                        {js_array(overhead_values)},

                    backgroundColor:
                        'rgba(255, 159, 64, 0.8)',

                    borderWidth: 1

                }}]

            }},

            options: {{

                responsive: true,

                plugins: {{

                    title: {{

                        display: true,

                        text:
                            '首次构建额外开销 - 越低越好'

                    }},

                    legend: {{

                        display: false

                    }}

                }},

                scales: {{

                    y: {{

                        beginAtZero: true,

                        title: {{

                            display: true,

                            text:
                                '额外耗时（秒）'

                        }}

                    }}

                }}

            }}

        }}
    );
"""


    # ========================================================
    # HTTP Charts
    # ========================================================

    for test_type, data in chart_data.items():

        html += f"""
    new Chart(
        document.getElementById(
            'rps-chart-{test_type}'
        ),
        {{

            type: 'bar',

            data: {{

                labels:
                    {js_array(data['labels'])},

                datasets: [{{

                    label:
                        '每秒请求数',

                    data:
                        {js_array(data['rps'])},

                    backgroundColor:
                        {js_array(data['colors'])},

                    borderColor:
                        {js_array(data['colors'])},

                    borderWidth: 1

                }}]

            }},

            options: {{

                responsive: true,

                plugins: {{

                    title: {{

                        display: true,

                        text:
                            '每秒请求数 (RPS) - 越高越好'

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

        }}
    );


    new Chart(
        document.getElementById(
            'latency-chart-{test_type}'
        ),
        {{

            type: 'bar',

            data: {{

                labels:
                    {js_array(data['labels'])},

                datasets: [{{

                    label:
                        '平均响应时间 (ms)',

                    data:
                        {js_array(data['latency'])},

                    backgroundColor:
                        {js_array(data['colors'])},

                    borderColor:
                        {js_array(data['colors'])},

                    borderWidth: 1

                }}]

            }},

            options: {{

                responsive: true,

                plugins: {{

                    title: {{

                        display: true,

                        text:
                            '平均响应时间 (ms) - 越低越好'

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

        }}
    );

"""


    # ========================================================
    # HTML End
    # ========================================================

    html += """
</script>

</body>

</html>
"""


    # ========================================================
    # Write
    # ========================================================

    with open(
        OUTPUT_FILE,
        'w',
        encoding='utf-8'
    ) as f:

        f.write(html)

    print(
        f"报告已生成: {OUTPUT_FILE}"
    )

    return OUTPUT_FILE