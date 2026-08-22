#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
解析wrk测试结果并生成性能对比报告

此脚本从target/wrk-results目录读取：
1. wrk性能测试结果
2. 各框架构建耗时

最终生成包含：
1. HTTP性能对比
2. 构建耗时对比
3. 详细测试数据
4. 测试结论

的HTML报告。
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


# 构建耗时名称
BUILD_FRAMEWORK_NAMES = {
    'feat': 'Feat',
    'vertx': 'Vert.x',
    'springboot': 'Spring Boot'
}


# ============================================================
# 测试类型映射
# ============================================================

TEST_TYPES = {
    'hello': 'Hello World',
    'json': 'JSON响应'
}


# ============================================================
# 颜色配置
# ============================================================

COLORS = {
    'feat': 'rgba(54, 162, 235, 0.8)',
    'vertx': 'rgba(255, 99, 132, 0.8)',
    'springboot': 'rgba(75, 192, 192, 0.8)',
    'quarkus': 'rgba(153, 102, 255, 0.8)'
}


# ============================================================
# wrk数据解析
# ============================================================

def parse_size_to_kb(value, unit):
    """将wrk传输速率单位转换为KB/s。"""

    multipliers = {
        'B': 1 / 1024,
        'KB': 1,
        'MB': 1024,
        'GB': 1024 * 1024,
    }

    return float(value) * multipliers.get(unit, 1)


def parse_time_to_ms(value, unit):
    """将wrk延迟单位转换为毫秒。"""

    multipliers = {
        'us': 0.001,
        'ms': 1,
        's': 1000,
    }

    return float(value) * multipliers.get(unit, 1)


def parse_wrk_result(file_path):
    """解析wrk结果文件，提取关键性能指标。"""

    try:
        with open(file_path, 'r') as f:
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
# 收集wrk测试结果
# ============================================================

def collect_results():
    """收集所有wrk测试结果。"""

    results = {}

    if not os.path.exists(RESULTS_DIR):
        print(
            f"错误: 结果目录 {RESULTS_DIR} 不存在"
        )
        return results

    for filename in os.listdir(RESULTS_DIR):

        if not filename.endswith('.txt'):
            continue

        # build-times.txt不是wrk测试文件
        if filename == 'build-times.txt':
            continue

        file_path = os.path.join(
            RESULTS_DIR,
            filename
        )

        # 例如：
        # feat-hello.txt
        # vertx-json.txt
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
# 解析构建耗时
# ============================================================

def parse_build_times():
    """
    解析构建耗时。

    build-times.txt格式：

    feat=12345
    vertx=23456
    springboot=34567
    quarkus_prepare=100
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
# HTML工具
# ============================================================

def js_array(values):
    """将Python列表转换为简单JS数组。"""

    return str(values).replace(
        "'",
        '"'
    )


# ============================================================
# HTML报告
# ============================================================

def generate_html_report(
    results,
    build_times=None
):
    """生成HTML性能报告。"""

    if build_times is None:
        build_times = parse_build_times()

    os.makedirs(
        os.path.dirname(OUTPUT_FILE),
        exist_ok=True
    )

    # ========================================================
    # HTTP图表数据
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
    # 构建耗时图表数据
    # ========================================================

    build_labels = []
    build_values = []
    build_colors = []

    for framework in (
        'feat',
        'vertx',
        'springboot'
    ):

        if framework not in build_times:
            continue

        build_labels.append(
            BUILD_FRAMEWORK_NAMES[framework]
        )

        build_values.append(
            round(
                build_times[framework] / 1000,
                2
            )
        )

        build_colors.append(
            COLORS[framework]
        )

    # ========================================================
    # HTML头
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
        测试参数：
        4 个线程，100 个连接，
        持续 60 秒，开启延迟统计。
    </p>

    <div class="info">

        <strong>构建耗时说明：</strong>

        Feat、Vert.x 和 Spring Boot
        统计的是各自执行
        <code>mvn clean install -DskipTests</code>
        的实际墙钟时间。

        Quarkus 当前使用仓库中已有的
        <code>quarkus-app.zip</code>
        预构建包，因此不计入编译耗时。

    </div>
"""

    # ========================================================
    # 构建耗时
    # ========================================================

    html += """
    <h2>构建耗时对比</h2>

    <div class="chart-full">

        <canvas id="build-time-chart"></canvas>

    </div>

    <h3>构建耗时明细</h3>

    <table>

        <tr>
            <th>框架</th>
            <th>构建方式</th>
            <th>耗时</th>
        </tr>
"""

    for framework in (
        'feat',
        'vertx',
        'springboot'
    ):

        if framework not in build_times:
            continue

        milliseconds = build_times[
            framework
        ]

        seconds = milliseconds / 1000

        html += f"""
        <tr>

            <td>
                {BUILD_FRAMEWORK_NAMES[framework]}
            </td>

            <td>
                Maven clean install
            </td>

            <td>
                <span class="highlight">
                    {seconds:.2f} 秒
                </span>

                <span class="muted">
                    ({milliseconds:.0f} ms)
                </span>
            </td>

        </tr>
"""

    html += """
        <tr>

            <td>Quarkus</td>

            <td>
                预构建 quarkus-app.zip
            </td>

            <td class="muted">
                未统计编译耗时
            </td>

        </tr>

    </table>
"""

    # ========================================================
    # HTTP性能图表
    # ========================================================

    html += """
    <h2>HTTP 性能对比图表</h2>
"""

    for test_type, data in chart_data.items():

        html += f"""
    <h3>
        {TEST_TYPES[test_type]}接口测试
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
    # HTTP详细数据
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
    # HTTP结论
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
    # 构建耗时结论
    # ========================================================

    if build_times:

        valid_build_times = {
            key: value
            for key, value in build_times.items()
            if key in BUILD_FRAMEWORK_NAMES
        }

        if valid_build_times:

            fastest_framework = min(
                valid_build_times,
                key=valid_build_times.get
            )

            fastest_time = (
                valid_build_times[
                    fastest_framework
                ] / 1000
            )

            html += f"""
            <li>
                在本次构建测试中，
                <strong>
                    {BUILD_FRAMEWORK_NAMES[
                        fastest_framework
                    ]}
                </strong>
                构建耗时最低，
                为
                <strong>
                    {fastest_time:.2f}
                </strong>
                秒。
            </li>
"""

            # ------------------------------------------------
            # Feat相对其他框架的构建耗时
            # ------------------------------------------------

            if 'feat' in valid_build_times:

                feat_time = valid_build_times[
                    'feat'
                ]

                for framework in (
                    'vertx',
                    'springboot'
                ):

                    if framework not in valid_build_times:
                        continue

                    other_time = valid_build_times[
                        framework
                    ]

                    if feat_time < other_time:

                        improvement = (
                            (
                                other_time
                                - feat_time
                            )
                            / other_time
                        ) * 100

                        html += f"""
            <li>
                Feat 相比
                {BUILD_FRAMEWORK_NAMES[framework]}
                构建耗时低
                <strong>
                    {improvement:.2f}%
                </strong>。
            </li>
"""

                    elif feat_time > other_time:

                        overhead = (
                            (
                                feat_time
                                - other_time
                            )
                            / other_time
                        ) * 100

                        html += f"""
            <li>
                Feat 相比
                {BUILD_FRAMEWORK_NAMES[framework]}
                构建耗时高
                <strong>
                    {overhead:.2f}%
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
            构建耗时为 CI 环境中的实际墙钟时间，
            会受到 Maven 缓存、依赖下载、
            GitHub Actions Runner 负载等因素影响。
        </p>

    </div>

</div>

<script>
"""

    # ========================================================
    # 构建耗时Chart
    # ========================================================

    html += f"""
    new Chart(
        document.getElementById(
            'build-time-chart'
        ),
        {{

            type: 'bar',

            data: {{

                labels: {js_array(
                    build_labels
                )},

                datasets: [{{

                    label:
                        '构建耗时（秒）',

                    data: {js_array(
                        build_values
                    )},

                    backgroundColor:
                        {js_array(
                            build_colors
                        )},

                    borderColor:
                        {js_array(
                            build_colors
                        )},

                    borderWidth: 1

                }}]

            }},

            options: {{

                responsive: true,

                plugins: {{

                    title: {{

                        display: true,

                        text:
                            'Maven 构建耗时（越低越好）'

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
    # HTTP Charts
    # ========================================================

    for test_type, data in chart_data.items():

        html += f"""
    // ========================================================
    // {TEST_TYPES[test_type]} RPS
    // ========================================================

    new Chart(
        document.getElementById(
            'rps-chart-{test_type}'
        ),
        {{

            type: 'bar',

            data: {{

                labels:
                    {js_array(
                        data['labels']
                    )},

                datasets: [{{

                    label:
                        '每秒请求数',

                    data:
                        {js_array(
                            data['rps']
                        )},

                    backgroundColor:
                        {js_array(
                            data['colors']
                        )},

                    borderColor:
                        {js_array(
                            data['colors']
                        )},

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


    // ========================================================
    // {TEST_TYPES[test_type]} Latency
    // ========================================================

    new Chart(
        document.getElementById(
            'latency-chart-{test_type}'
        ),
        {{

            type: 'bar',

            data: {{

                labels:
                    {js_array(
                        data['labels']
                    )},

                datasets: [{{

                    label:
                        '平均响应时间 (ms)',

                    data:
                        {js_array(
                            data['latency']
                        )},

                    backgroundColor:
                        {js_array(
                            data['colors']
                        )},

                    borderColor:
                        {js_array(
                            data['colors']
                        )},

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
    # HTML结束
    # ========================================================

    html += """
</script>

</body>

</html>
"""

    # ========================================================
    # 写入HTML
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