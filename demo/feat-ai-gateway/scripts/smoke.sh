#!/bin/sh
# 本地联调用的验证脚本（不参与打包）
KEY="sk-feat-dev-123456"
HOST="http://127.0.0.1:8080"

echo "== 1. 无 key =="
curl -s -o /tmp/gw_401.json -w "status=%{http_code}\n" -X POST "$HOST/v1/chat/completions" \
  -H "Content-Type: application/json" \
  -d '{"model":"mock-chat","messages":[{"role":"user","content":"hi"}]}'
cat /tmp/gw_401.json; echo

echo "== 2. 非流式 =="
curl -s -o /tmp/gw_full.json -w "status=%{http_code} time=%{time_total}s\n" -X POST "$HOST/v1/chat/completions" \
  -H "Authorization: Bearer $KEY" -H "Content-Type: application/json" \
  -d '{"model":"mock-chat","messages":[{"role":"user","content":"hi"}]}'
head -c 300 /tmp/gw_full.json; echo

echo "== 3. 流式 =="
curl -s -N -o /tmp/gw_stream.txt -w "status=%{http_code} time=%{time_total}s\n" -X POST "$HOST/v1/chat/completions" \
  -H "Authorization: Bearer $KEY" -H "Content-Type: application/json" \
  -d '{"model":"mock-chat","stream":true,"messages":[{"role":"user","content":"hi"}]}'
echo "首行: $(head -1 /tmp/gw_stream.txt | cut -c1-120)"
echo "行数: $(wc -l < /tmp/gw_stream.txt)"
echo "末尾: $(tail -2 /tmp/gw_stream.txt | tr '\n' ' ' | cut -c1-160)"

echo "== 4. 未知模型 =="
curl -s -w "\nstatus=%{http_code}\n" -X POST "$HOST/v1/chat/completions" \
  -H "Authorization: Bearer $KEY" -H "Content-Type: application/json" \
  -d '{"model":"nope","messages":[]}'

echo "== 5. /metrics 关键行 =="
curl -s "$HOST/metrics" | grep -E "requests_total|ttft_ms_sum|ttft_ms_count|tokens_total|heap_used|inflight"

echo "== 6. /admin/stats =="
curl -s "$HOST/admin/stats"; echo
