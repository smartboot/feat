/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.demo;

import tech.smartboot.feat.Feat;
import tech.smartboot.feat.ai.FeatAI;
import tech.smartboot.feat.ai.ModelMeta;
import tech.smartboot.feat.ai.chat.ChatModel;
import tech.smartboot.feat.ai.chat.entity.ResponseMessage;
import tech.smartboot.feat.ai.chat.entity.StreamResponseCallback;
import tech.smartboot.feat.core.server.HttpResponse;
import tech.smartboot.feat.core.server.upgrade.sse.SSEUpgrade;
import tech.smartboot.feat.core.server.upgrade.sse.SseEmitter;
import tech.smartboot.feat.router.Router;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class WeChatDemo extends BaseChat {
    public static void main(String[] args) throws IOException {
        File file = new File("pages/src/content/docs/");
        Set<String> ig = new HashSet<>();
        ig.add("client");
        ig.add("cloud");
        ig.add("server");
        StringBuilder stringBuilder = new StringBuilder();
        loadFile(file, stringBuilder);
        StringBuilder sourceBuilder = new StringBuilder();
        ig.clear();
        ig.add("chat");
        ig.add("milvus");
        ig.add("expression");
//        loadSource(new File("feat-ai/src/main/java/tech/smartboot/feat/ai/"), sourceBuilder);
        loadSource(new File("feat-core/src/main/java/tech/smartboot/feat/core/client"), sourceBuilder);

        StringBuilder demoBuilder = new StringBuilder();
        loadSource(new File("feat-ai/src/test/java/tech/smartboot/feat/ai/test/"), demoBuilder);
        ChatModel chatModel = FeatAI.chatModel(opts -> {
            opts.model(ModelMeta.GiteeAI.DeepSeek_R1_Distill_Qwen_32B).system("你是一个负责Feat微信公众号的编辑人员，你的任务是根据用户要求编写微信公众号文章。"
                                    + "Feat参考内容为：\n" + stringBuilder
                                    + "\n FeatClient的实现源码为：\n" + sourceBuilder
//                    + "\n 示例代码为：" + demoBuilder
                    )
                    .debug(true);
        });


        Router router = new Router();
        router.route("/", ctx -> {
            HttpResponse response = ctx.Response;
            response.setContentType("text/html");
            InputStream inputStream = Feat.class.getClassLoader().getResourceAsStream("static/project_doc_ai.html");
            byte[] buffer = new byte[1024];
            int length = 0;
            while ((length = inputStream.read(buffer)) != -1) {
                response.write(buffer, 0, length);
            }
        });
        router.route("/chat", ctx -> {
            ctx.Request.upgrade(new SSEUpgrade() {
                public void onOpen(SseEmitter sseEmitter) {
                    chatModel.chatStream(ctx.Request.getParameter("content"), new StreamResponseCallback() {

                        @Override
                        public void onCompletion(ResponseMessage responseMessage) {
                            responseMessage.discard();
                            if (!responseMessage.isSuccess()) {
                                return;
                            }
                            String content = responseMessage.getContent();
                            if (true) {
                                return;
                            }
                            try {
                                sseEmitter.send(SseEmitter.event().data("<br/>开始优化文章...<br/>"));
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            FeatAI.chatModel(opts -> {
                                opts.model(ModelMeta.GiteeAI.DeepSeek_R1_Distill_Qwen_32B).system("你是一个负责Feat微信公众号的编辑人员，你的任务是根据用户要求编写微信公众号文章。").debug(true);
                            }).chatStream("站在读者角度，优化大模型生成的微信公众号文章：\n" + content, new StreamResponseCallback() {

                                @Override
                                public void onCompletion(ResponseMessage responseMessage) {
                                    sseEmitter.complete();
                                }

                                @Override
                                public void onStreamResponse(String content) {
                                    try {
                                        sseEmitter.send(SseEmitter.event().data(toHtml(content)));
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                }
                            });
                        }

                        @Override
                        public void onStreamResponse(String content) {
                            System.out.print(content);
                            try {
                                sseEmitter.send(SseEmitter.event().data(toHtml(content)));
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    });
                }
            });
        });
        Feat.httpServer(opt -> opt.debug(false).readBufferSize(1024 * 1024)).httpHandler(router).listen(8080);
    }
}
