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


import tech.smartboot.feat.Feat;
import tech.smartboot.feat.cloud.FeatCloud;

/**
 * @author 三刀(zhengjunweimail@163.com)
 * @version v1.0.0
 */
public class Bootstrap {

    public static void main(String[] args) {
        int cpuNum = Runtime.getRuntime().availableProcessors();
        // 定义服务器接受的消息类型以及各类消息对应的处理器
//        Feat.createHttpServer(options -> {
//            options.threadNum(cpuNum + 1)
//                    .headerLimiter(0)
//                    .readBufferSize(1024 * 4)
//                    .writeBufferSize(1024 * 4);
//        }).httpHandler(request -> {
//            HttpResponse response = request.getResponse();
//            if ("/plaintext".equals(request.getRequestURI())) {
//                response.setContentLength(body.length);
//                response.setContentType(HeaderValueEnum.ContentType.TEXT_PLAIN_UTF8);
//                response.write(body);
//            } else if ("/json".equals(request.getRequestURI())) {
//                response.setContentType("application/json");
//                JsonUtil.writeJsonBytes(response, new Message("Hello, World!"));
//            }
//        }).listen(8080);
        FeatCloud.cloudServer(options -> {
            options.threadNum(cpuNum + 1)
                    .headerLimiter(0)
                    .readBufferSize(1024 * 4)
                    .writeBufferSize(1024 * 4);
        }).listen(8080);
    }

}
