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
import tech.smartboot.feat.core.server.upgrade.sse.SSEUpgrade;
import tech.smartboot.feat.core.server.upgrade.sse.SseEmitter;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author 三刀(zhengjunweimail@163.com)
 * @version v1.0.0
 */
public class SSEDemo {
    public static void main(String[] args) throws Exception {
        Feat.httpServer(serverOptions -> serverOptions.debug(true)).httpHandler(req -> {
            req.upgrade(new SSEUpgrade() {
                public void onOpen(SseEmitter sseEmitter) {
                    SSEUpgrade handler = this;
//                System.out.println("receive...:" + uid);
                    Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(new Runnable() {
                        int i = 0;

                        @Override
                        public void run() {
                            try {
                                sseEmitter.send(SseEmitter.event().name("update").comment("aaa").id(String.valueOf(i++)).data("hello world"));
                                if (i == 10) {
                                    sseEmitter.complete();
                                }
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }, 1, 1, TimeUnit.SECONDS);
                }
            });
        }).listen(8080);
    }
}
