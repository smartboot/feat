package tech.smartboot.feat.demo.ai;

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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class WeChatDemo {
    public static void main(String[] args) throws IOException {
        File file = new File("pages/src/content");
        StringBuilder stringBuilder = new StringBuilder();
        loadFile(file, stringBuilder);
        StringBuilder sourceBuilder = new StringBuilder();
        loadSource(new File("feat-core/src/main/java/tech/smartboot/feat/"), sourceBuilder);

        StringBuilder demoBuilder = new StringBuilder();
        loadSource(new File("feat-test/src/main/java/tech/smartboot/feat/demo/"), demoBuilder);
        ChatModel chatModel = FeatAI.chatModel(opts -> {
            opts.model(ModelMeta.GITEE_AI_DeepSeek_R1_Distill_Qwen_32B).system("你是一个负责Feat微信公众号的编辑人员，你的任务是根据用户要求编写微信公众号文章。" + "Feat参考内容为：\n" + stringBuilder + "\n FeatClient的实现源码为：\n" + sourceBuilder + "\n 示例代码为：" + demoBuilder).debug(true);
        });


        Router router = new Router();
        router.route("/", req -> {
            HttpResponse response = req.getResponse();
            response.setContentType("text/html");
            InputStream inputStream = WeChatDemo.class.getClassLoader().getResourceAsStream("static/project_doc_ai.html");
            byte[] buffer = new byte[1024];
            int length = 0;
            while ((length = inputStream.read(buffer)) != -1) {
                response.write(buffer, 0, length);
            }
        });
        router.route("/chat", req -> {
            req.upgrade(new SSEUpgrade() {
                public void onOpen(SseEmitter sseEmitter) {
                    chatModel.chatStream(req.getParameter("content"), new StreamResponseCallback() {

                        @Override
                        public void onCompletion(ResponseMessage responseMessage) {
                            responseMessage.discard();
                            if (!responseMessage.isSuccess()) {
                                return;
                            }
                            String content = responseMessage.getContent();
                            try {
                                sseEmitter.send(SseEmitter.event().data("<br/>开始优化文章...<br/>"));
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            FeatAI.chatModel(opts -> {
                                opts.model(ModelMeta.GITEE_AI_DeepSeek_R1_Distill_Qwen_32B).system("你是一个负责Feat微信公众号的编辑人员，你的任务是根据用户要求编写微信公众号文章。").debug(true);
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
        Feat.httpServer(opt -> opt.debug(false)).httpHandler(router).listen(8080);
    }

    public static String toHtml(String content) {
        return content.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br/>").replace("\r", "<br/>").replace(" ", "&nbsp;");
    }

    public static void loadFile(File file, StringBuilder sb) throws IOException {
        for (File f : file.listFiles()) {
            if (f.isDirectory()) {
                loadFile(f, sb);
            }
            if (f.isFile() && f.getName().endsWith(".mdx")) {
                try (FileInputStream fis = new FileInputStream(f);) {
                    byte[] bytes = new byte[1024];
                    int len;
                    while ((len = fis.read(bytes)) != -1) {
                        sb.append(new String(bytes, 0, len));
                    }
                }
            }
        }
    }

    public static void loadSource(File file, StringBuilder sb) throws IOException {
        for (File f : file.listFiles()) {
//            if (f.isDirectory()) {
//                loadFile(f, sb);
//            }
            if (f.isFile() && f.getName().endsWith(".java")) {
                sb.append("## " + f.getName() + "\n");
                sb.append("```java\n");
                try (FileInputStream fis = new FileInputStream(f);) {
                    byte[] bytes = new byte[1024];
                    int len;
                    while ((len = fis.read(bytes)) != -1) {
                        sb.append(new String(bytes, 0, len));
                    }
                }
                sb.append("\n```\n");
            }
        }
    }
}
