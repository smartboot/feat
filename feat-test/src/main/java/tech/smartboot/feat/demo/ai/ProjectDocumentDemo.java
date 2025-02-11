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

public class ProjectDocumentDemo {
    public static void main(String[] args) throws IOException {
        File file = new File("pages/src/content");
        StringBuilder docs = new StringBuilder();
        loadFile(file, docs);

        StringBuilder sourceBuilder = new StringBuilder();
        loadSource(new File("feat-core/src/main/java/tech/smartboot/feat/ai"), sourceBuilder);

        StringBuilder demoBuilder = new StringBuilder();
        loadSource(new File("feat-test/src/main/java/tech/smartboot/feat/demo/ai"), demoBuilder);
        ChatModel chatModel = FeatAI.chatModel(opts -> {
            opts.model(ModelMeta.GITEE_AI_DeepSeek_R1_Distill_Qwen_32B)
                    .system("你主要负责为这个项目编写使用文档，根据用户要求编写相关章节内容。" +
                            "参考内容为：\n" + docs
                            + "\n 实现源码为：\n" + sourceBuilder
                            + "\n 示例代码为：" + demoBuilder)
            ;
        });


        Router router = new Router();
        router.route("/", req -> {
            HttpResponse response = req.getResponse();
            response.setContentType("text/html");
            InputStream inputStream = ProjectDocumentDemo.class.getClassLoader().getResourceAsStream("static/project_doc_ai.html");
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
                            sseEmitter.complete();
                        }

                        @Override
                        public void onStreamResponse(String content) {
                            System.out.print(content);
                            try {
                                sseEmitter.send(SseEmitter.event().data(content));
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    });
                }
            });
        });
        Feat.httpServer().httpHandler(router).listen(8080);
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
