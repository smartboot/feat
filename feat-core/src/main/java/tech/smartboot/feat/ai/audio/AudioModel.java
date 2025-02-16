package tech.smartboot.feat.ai.audio;

import tech.smartboot.feat.Feat;
import tech.smartboot.feat.core.client.HttpPost;
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.handler.BaseHttpHandler;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class AudioModel {
    public static void main(String[] args) throws FileNotFoundException {
        Feat.httpServer().httpHandler(new BaseHttpHandler() {
            @Override
            public void handle(HttpRequest request, CompletableFuture<Object> completableFuture) throws Throwable {
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
        }).listen();
    }
}
