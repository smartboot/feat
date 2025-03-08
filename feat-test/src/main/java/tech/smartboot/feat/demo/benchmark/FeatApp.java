/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.demo.benchmark;

import org.smartboot.socket.extension.plugins.MonitorPlugin;
import org.smartboot.socket.extension.plugins.StreamMonitorPlugin;
import tech.smartboot.feat.cloud.FeatCloud;
import tech.smartboot.feat.cloud.annotation.Controller;
import tech.smartboot.feat.cloud.annotation.RequestMapping;
import tech.smartboot.feat.core.common.HeaderValue;
import tech.smartboot.feat.core.server.HttpResponse;

/**
 * @author 三刀(zhengjunweimail @ 163.com)
 * @version v1.0.0
 */
@Controller
public class FeatApp {

    @RequestMapping("/hello")
    public String plaintext(HttpResponse response) {
        response.setContentType(HeaderValue.ContentType.TEXT_PLAIN_UTF8);
        return "Hello World!";
    }

    @RequestMapping("/json")
    public Response json(HttpResponse response) {
        response.setContentType(HeaderValue.ContentType.APPLICATION_JSON_UTF8);
        return new Response("Hello", "World");
    }

    public static void main(String[] args) {
        int cpuNum = Runtime.getRuntime().availableProcessors();
        // 定义服务器接受的消息类型以及各类消息对应的处理器
        FeatCloud.cloudServer(options -> {
            options
                    .setPackages("tech.smartboot.feat.demo.benchmark")
                    .threadNum(cpuNum + 1)
                    .addPlugin(new MonitorPlugin<>(5))
//                    .headerLimiter(0)
//                    .debug(true)
                    .readBufferSize(1024 * 4)
                    .writeBufferSize(1024 * 4);
        }).listen(8082);
    }
}
