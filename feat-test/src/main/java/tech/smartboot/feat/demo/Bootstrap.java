/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.demo;

import tech.smartboot.feat.Feat;
import tech.smartboot.feat.core.server.HttpResponse;
import tech.smartboot.feat.core.server.HttpServer;
import tech.smartboot.feat.router.Router;

/**
 * @author 三刀(zhengjunweimail@163.com)
 * @version v1.0.0
 */
public class Bootstrap {
    static byte[] body = "Hello, World!".getBytes();

    public static void main(String[] args) {
        Router routeHandle = new Router();
        routeHandle.route("/plaintext", ctx -> {
            HttpResponse response = ctx.Response;
            response.setContentLength(body.length);
            response.setContentType("text/plain; charset=UTF-8");
            response.write(body);
        });
        int cpuNum = Runtime.getRuntime().availableProcessors();

        // 定义服务器接受的消息类型以及各类消息对应的处理器
        HttpServer bootstrap = Feat.httpServer(opt -> opt.threadNum(cpuNum).debug(false).headerLimiter(0).readBufferSize(1024 * 4).writeBufferSize(1024 * 4));
        bootstrap.httpHandler(routeHandle).listen(8080);
    }
}
