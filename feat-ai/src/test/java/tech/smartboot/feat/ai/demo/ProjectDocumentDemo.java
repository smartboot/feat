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

public class ProjectDocumentDemo extends BaseChat {
    public static void main(String[] args) throws IOException {
        File file = new File("pages/src/content/docs/server");
        Set<String> ignoreDoc = new HashSet<>();
        ignoreDoc.add("client");
        ignoreDoc.add("cloud");
        ignoreDoc.add("ai");
        StringBuilder docs = new StringBuilder();
        loadFile(file, ignoreDoc, docs);

        Set<String> ignore = new HashSet<>();
        ignore.add("milvus");
        StringBuilder sourceBuilder = new StringBuilder();
        loadSource(new File("feat-core/src/main/java/tech/smartboot/feat/ai/vector"), ignore, sourceBuilder);

        StringBuilder demoBuilder = new StringBuilder();
        Set<String> ignore1 = new HashSet<>();
        ignore1.add("ai");
        ignore1.add("apt");
        ignore1.add("client");
        loadSource(new File("feat-test/src/test/java/tech/smartboot/feat/test/ai"), ignore1, demoBuilder);
        ChatModel chatModel = FeatAI.chatModel(opts -> {
            opts.model(ModelMeta.GITEE_AI_DeepSeek_R1_Distill_Qwen_32B)
                    .system("你主要负责为这个项目编写使用文档，根据用户要求编写相关章节内容。"
                            + "参考内容为：\n" + docs
                                    + "\n 实现源码为：\n" + sourceBuilder
                            + "\n 示例代码为：" + demoBuilder
                    )
                    .debug(true)
            ;
        });


        Router router = new Router();
        router.route("/", ctx -> {
            HttpResponse response = ctx.Response;
            response.setContentType("text/html");
            InputStream inputStream = ProjectDocumentDemo.class.getClassLoader().getResourceAsStream("static/project_doc_ai.html");
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
                            try {
                                sseEmitter.send(SseEmitter.event().data("<br/>开始阅读文章初稿...<br/>"));
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
//                            FeatAI.chatModel(opts -> {
//                                opts.model(ModelMeta.GITEE_AI_DeepSeek_R1_Distill_Qwen_32B)
//                                        .system("你是这篇文章的目标读者，可以反馈你对于文章内容的改进意见，文章内容为：\n" + responseMessage.getContent()
//                                        )
//                                        .debug(true)
//                                ;
//                            }).chatStream("指出你觉得需要优化的部分", new StreamResponseCallback() {
//
//                                @Override
//                                public void onCompletion(ResponseMessage responseMessage) {
//                                    try {
//                                        sseEmitter.send(SseEmitter.event().data("<br/>阅读完毕...<br/>"));
//                                    } catch (IOException e) {
//                                        throw new RuntimeException(e);
//                                    }
//                                    chatModel.chatStream("根据读者的改进建议优化文章内容：\n" + responseMessage.getContent(), new StreamResponseCallback() {
//
//                                        @Override
//                                        public void onCompletion(ResponseMessage responseMessage) {
//                                            sseEmitter.complete();
//                                        }
//
//                                        @Override
//                                        public void onStreamResponse(String content) {
//                                            try {
//                                                sseEmitter.send(SseEmitter.event().data(toHtml(content)));
//                                            } catch (IOException e) {
//                                                throw new RuntimeException(e);
//                                            }
//                                        }
//                                    });
//                                }
//
//                                @Override
//                                public void onStreamResponse(String content) {
//                                    try {
//                                        sseEmitter.send(SseEmitter.event().data(toHtml(content)));
//                                    } catch (IOException e) {
//                                        throw new RuntimeException(e);
//                                    }
//                                }
//                            });
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
        Feat.httpServer(opt -> opt.debug(false)).httpHandler(router).listen(8080);
    }


}
