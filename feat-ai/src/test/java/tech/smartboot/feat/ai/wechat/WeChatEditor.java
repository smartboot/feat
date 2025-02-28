package tech.smartboot.feat.ai.wechat;

import tech.smartboot.feat.ai.FeatAI;
import tech.smartboot.feat.ai.ModelMeta;
import tech.smartboot.feat.ai.chat.ChatModel;
import tech.smartboot.feat.ai.demo.BaseChat;
import tech.smartboot.feat.ai.prompt.PromptTemplate;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;

public class WeChatEditor extends BaseChat {
    public static void main(String[] args) throws IOException {

        ChatModel chatModel = FeatAI.chatModel(opts -> {
            opts
//                    .model("qwen2.5:3b")
//                    .baseUrl("http://localhost:11434/v1") // Ollama本地服务地址
                    .model(ModelMeta.GITEE_AI_DeepSeek_R1_Distill_Qwen_32B)
                    .debug(true)
            ;
        });

        StringBuilder sourceBuilder = new StringBuilder();
        //源码
        File sourcePath = new File("./feat-ai/src/main");
        sourceBuilder.append("---source begin---\n");
        Files.walkFileTree(sourcePath.toPath(), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                readFile(file.toFile(), sourceBuilder);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (Arrays.asList("audio", "embedding", "vector").contains(dir.getFileName().toString())) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return super.preVisitDirectory(dir, attrs);
            }
        });
        sourceBuilder.append("---source end---\n");

        File demoPath = new File("./feat-ai/src/test/java");
        sourceBuilder.append("---demo begin---\n");
        Files.walkFileTree(demoPath.toPath(), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                readFile(file.toFile(), sourceBuilder);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (Arrays.asList("demo", "test").contains(dir.getFileName().toString())) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }
        });
        sourceBuilder.append("---demo end---\n");
        System.out.println(sourceBuilder);

        chatModel.chat(PromptTemplate.WECHAT_EDITOR, data -> {
            data.put("topic", "通过Feat AI与大模型技术的结合，未来将打造一款智能应用，基于开源项目的源码自动生成项目文档。此次以Feat项目自身为例，通过解析工程中的pom.xml文件，自动生成项目架构图。在文章内容中解读其实现原理");
            data.put("reference", sourceBuilder.toString());
        }).whenComplete((responseMessage, throwable) -> {
            System.out.println(responseMessage.getContent());
        });


    }
}
