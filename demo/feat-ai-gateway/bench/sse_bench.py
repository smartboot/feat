#!/usr/bin/env python3
"""
feat-ai-gateway P0 压测：只依赖标准库，不装任何东西。

它测四件事（也是设计文档 §9 的验收标准）：
  1. N 个并发流式连接能不能同时建立并跑完
  2. 网关 JVM 堆增长多少（从 /metrics 采）
  3. 首字延迟（TTFT）分布 —— 端到端，含网关自身开销
  4. 跑完后在途连接是否归零

用法：
  python3 bench/sse_bench.py --concurrency 1000 --requests 1000
  python3 bench/sse_bench.py --concurrency 200 --requests 400 --nonstream

注意：并发数受 `ulimit -n` 限制，macOS 默认可能只有 256，先 `ulimit -n 10240`。
"""

import argparse
import asyncio
import json
import statistics
import time
import urllib.request
from urllib.parse import urlparse


def percentile(values, p):
    if not values:
        return 0.0
    ordered = sorted(values)
    k = max(0, min(len(ordered) - 1, int(round((p / 100.0) * (len(ordered) - 1)))))
    return ordered[k]


async def one_request(host, port, key, model, stream, results, index):
    body = json.dumps({
        "model": model,
        "stream": stream,
        "messages": [{"role": "user", "content": "benchmark"}],
    }).encode()
    headers = (
        "POST /v1/chat/completions HTTP/1.1\r\n"
        "Host: {host}\r\n"
        "Authorization: Bearer {key}\r\n"
        "Content-Type: application/json\r\n"
        "Content-Length: {length}\r\n"
        "Connection: close\r\n\r\n"
    ).format(host=host, key=key, length=len(body))

    started = time.perf_counter()
    ttft = None
    received = 0
    status = None
    try:
        reader, writer = await asyncio.open_connection(host, port)
        writer.write(headers.encode() + body)
        await writer.drain()

        head = await reader.readuntil(b"\r\n\r\n")
        first_line = head.split(b"\r\n", 1)[0].decode(errors="replace")
        parts = first_line.split(" ")
        status = int(parts[1]) if len(parts) > 1 else 0
        if status != 200:
            payload = await reader.read(4096)
            results.append({"ok": False, "status": status, "body": payload[:200].decode(errors="replace")})
            writer.close()
            return

        while True:
            chunk = await reader.read(8192)
            if not chunk:
                break
            received += len(chunk)
            if ttft is None and (b"data:" in chunk or not stream):
                ttft = (time.perf_counter() - started) * 1000
            if stream and b"[DONE]" in chunk:
                break
        writer.close()
        total = (time.perf_counter() - started) * 1000
        results.append({
            "ok": True,
            "status": status,
            "ttft": ttft if ttft is not None else total,
            "total": total,
            "bytes": received,
        })
    except Exception as exc:  # noqa: BLE001 - 压测脚本要记录所有失败类型
        results.append({"ok": False, "status": status, "body": repr(exc)[:200]})


def scrape(url):
    try:
        with urllib.request.urlopen(url, timeout=2) as resp:
            text = resp.read().decode()
    except Exception:
        return {}
    values = {}
    for line in text.splitlines():
        if line.startswith("#") or not line.strip():
            continue
        name, _, value = line.partition(" ")
        try:
            values[name] = float(value)
        except ValueError:
            pass
    return values


async def sampler(metrics_url, samples, stop_event):
    while not stop_event.is_set():
        data = scrape(metrics_url)
        if data:
            samples.append(data)
        await asyncio.sleep(0.3)


async def run(args):
    parsed = urlparse(args.url)
    host = parsed.hostname or "127.0.0.1"
    port = parsed.port or 80
    metrics_url = "{}/metrics".format(args.url.rstrip("/"))

    before = scrape(metrics_url)
    results = []
    samples = []
    stop_event = asyncio.Event()
    sampler_task = asyncio.create_task(sampler(metrics_url, samples, stop_event))

    print("warmup...")
    warm = []
    for _ in range(min(20, args.requests)):
        await one_request(host, port, args.key, args.model, args.stream, warm, 0)
    print("warmup done: {}/{} ok".format(sum(1 for r in warm if r["ok"]), len(warm)))

    baseline_heap = scrape(metrics_url).get("feat_gateway_jvm_heap_used_bytes", 0)
    print("running: concurrency={} requests={} stream={}".format(args.concurrency, args.requests, args.stream))

    semaphore = asyncio.Semaphore(args.concurrency)
    started = time.perf_counter()

    async def worker(i):
        async with semaphore:
            await one_request(host, port, args.key, args.model, args.stream, results, i)

    await asyncio.gather(*(worker(i) for i in range(args.requests)))
    elapsed = time.perf_counter() - started

    stop_event.set()
    await sampler_task
    await asyncio.sleep(1.0)
    after = scrape(metrics_url)

    ok = [r for r in results if r["ok"]]
    failed = [r for r in results if not r["ok"]]
    ttfts = [r["ttft"] for r in ok]
    totals = [r["total"] for r in ok]
    peak_heap = max([s.get("feat_gateway_jvm_heap_used_bytes", 0) for s in samples] or [0])
    peak_inflight = max([s.get("feat_gateway_inflight_requests", 0) for s in samples] or [0])

    print()
    print("=" * 62)
    print("concurrency        : {}".format(args.concurrency))
    print("requests           : {}   ok={} failed={}".format(args.requests, len(ok), len(failed)))
    print("wall time          : {:.2f}s   throughput={:.1f} req/s".format(elapsed, args.requests / elapsed))
    if ttfts:
        print("TTFT  p50/p95/p99  : {:.0f} / {:.0f} / {:.0f} ms".format(
            percentile(ttfts, 50), percentile(ttfts, 95), percentile(ttfts, 99)))
        print("total p50/p95/p99  : {:.0f} / {:.0f} / {:.0f} ms".format(
            percentile(totals, 50), percentile(totals, 95), percentile(totals, 99)))
    print("heap baseline      : {:.1f} MB".format(baseline_heap / 1048576))
    print("heap peak          : {:.1f} MB".format(peak_heap / 1048576))
    print("heap growth        : {:.1f} MB".format((peak_heap - baseline_heap) / 1048576))
    print("peak inflight      : {:.0f}".format(peak_inflight))
    print("inflight after run : {:.0f}".format(after.get("feat_gateway_inflight_requests", -1)))
    print("gateway requests   : {:.0f} (before {:.0f}, after {:.0f})".format(
        after.get("feat_gateway_requests_total", 0) - before.get("feat_gateway_requests_total", 0),
        before.get("feat_gateway_requests_total", 0), after.get("feat_gateway_requests_total", 0)))
    print("gateway errors     : {:.0f}".format(
        after.get("feat_gateway_errors_total", 0) - before.get("feat_gateway_errors_total", 0)))
    if failed:
        print("failure samples    :")
        for item in failed[:5]:
            print("   status={} {}".format(item.get("status"), item.get("body")))
    print("=" * 62)


def main():
    parser = argparse.ArgumentParser(description="feat-ai-gateway P0 benchmark")
    parser.add_argument("--url", default="http://127.0.0.1:8080")
    parser.add_argument("--key", default="sk-feat-dev-123456")
    parser.add_argument("--model", default="mock-chat")
    parser.add_argument("--concurrency", type=int, default=200)
    parser.add_argument("--requests", type=int, default=200)
    parser.add_argument("--nonstream", action="store_true", help="测试非流式请求")
    args = parser.parse_args()
    args.stream = not args.nonstream
    asyncio.run(run(args))


if __name__ == "__main__":
    main()
