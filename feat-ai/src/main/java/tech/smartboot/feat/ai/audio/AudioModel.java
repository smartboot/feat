/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.audio;

import tech.smartboot.feat.Feat;
import tech.smartboot.feat.core.client.HttpPost;
import tech.smartboot.feat.core.server.HttpHandler;
import tech.smartboot.feat.core.server.HttpRequest;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class AudioModel {
    public static void main(String[] args) throws FileNotFoundException {
        Feat.httpServer().httpHandler(new HttpHandler() {
            @Override
            public void handle(HttpRequest request, CompletableFuture<Void> completableFuture) throws Throwable {
                Map<String, String> params = new HashMap<>();
                params.put("model", "ChatTTS");
                params.put("input", request.getParameter("content"));

                HttpPost post = Feat.postJson("https://ai.gitee.com/v1/audio/speech", opts -> {
                    opts.debug(true);
                }, header -> {
                }, params);
                post.onResponseHeader(resp -> {
                    request.getResponse().setContentType(resp.getContentType());
                    request.getResponse().setContentLength(resp.getContentLength());
                }).onResponseBody((response, bytes, end) -> {
                    request.getResponse().write(bytes);
                    if (end) {
                        completableFuture.complete(null);
                    }
                });
                post.onFailure(throwable -> throwable.printStackTrace()).submit();
            }

            @Override
            public void handle(HttpRequest request) throws Throwable {

            }
        }).listen();
    }
}
