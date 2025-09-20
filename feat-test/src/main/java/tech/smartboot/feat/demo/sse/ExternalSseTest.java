/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.demo.sse;

import tech.smartboot.feat.Feat;
import tech.smartboot.feat.core.client.HttpClient;
import tech.smartboot.feat.core.client.HttpResponse;
import tech.smartboot.feat.core.client.stream.ServerSentEventStream;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 使用外部SSE服务测试Feat的SSE客户端
 * 
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class ExternalSseTest {
    
    public static void main(String[] args) throws Exception {
        System.out.println("=== 使用外部SSE服务测试Feat客户端 ===");
        
        // 测试HTTPSse demo 
        testHttpsSseDemo();
        
        // 等待30秒接收事件
        Thread.sleep(30000);
        
        System.out.println("\\n=== 测试完成 ===");
    }
    
    /**
     * 测试公开的SSE demo服务
     */
    private static void testHttpsSseDemo() throws Exception {
        System.out.println("连接到SSE Demo服务...");
        
        AtomicInteger eventCount = new AtomicInteger(0);
        
        // 使用公开的SSE测试服务
        String url = "https://sse.dev/test";
        
        HttpClient httpClient = new HttpClient(url);
        
        httpClient.get("/")
                .header(h -> {
                    h.add("Accept", "text/event-stream");
                    h.add("Cache-Control", "no-cache");
                    h.add("User-Agent", "Feat-SSE-Client/1.0");
                })
                .onResponseBody(new ServerSentEventStream() {
                    @Override
                    public void onEvent(HttpResponse httpResponse, Map<String, String> event) {
                        int count = eventCount.incrementAndGet();
                        
                        String id = event.get("id");
                        String type = event.get("event");
                        String data = event.get("data");
                        String retry = event.get("retry");
                        
                        System.out.println("\\n[事件 #" + count + "]");
                        System.out.println("  ID: " + id);
                        System.out.println("  类型: " + type);
                        System.out.println("  数据: " + data);
                        System.out.println("  重试: " + retry);
                        System.out.println("  时间: " + System.currentTimeMillis());
                        
                        if (count >= 10) {
                            System.out.println("\\n已接收10个事件，测试结束");
                            System.exit(0);
                        }
                    }
                })
                .onSuccess(response -> {
                    System.out.println("✅ 连接成功！");
                    System.out.println("响应状态: " + response.statusCode());
                    System.out.println("Content-Type: " + response.getContentType());
                    System.out.println("开始接收事件...");
                })
                .onFailure(throwable -> {
                    System.err.println("❌ 连接失败: " + throwable.getMessage());
                    throwable.printStackTrace();
                })
                .submit();
    }
}