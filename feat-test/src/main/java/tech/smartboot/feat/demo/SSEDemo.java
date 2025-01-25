package tech.smartboot.feat.demo;

import tech.smartboot.feat.Feat;
import tech.smartboot.feat.restful.sse.SSEHandler;
import tech.smartboot.feat.restful.sse.SseEmitter;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SSEDemo {
    public static void main(String[] args) throws Exception {
        Feat.createHttpServer(serverOptions -> serverOptions.debug(true)).httpHandler(new SSEHandler() {
            @Override
            public void onOpen(SseEmitter sseEmitter) {
                SSEHandler handler = this;
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
        }).listen(8080);
    }
}
