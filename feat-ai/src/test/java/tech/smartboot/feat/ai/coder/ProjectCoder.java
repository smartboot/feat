package tech.smartboot.feat.ai.coder;

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

public class ProjectCoder extends BaseChat {
    public static void main(String[] args) throws IOException {

        ChatModel chatModel = FeatAI.chatModel(opts -> {
            opts
//                    .model("qwen2.5:3b")
//                    .baseUrl("http://localhost:11434/v1") // Ollama本地服务地址
                    .model(ModelMeta.GITEE_AI_DeepSeek_R1_Distill_Qwen_32B).debug(false);
        });

        File file1 = new File("./feat-core/src/main/java/tech/smartboot/feat/router");
        StringBuilder sources = new StringBuilder();
        Files.walkFileTree(file1.toPath(), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                readFile(file.toFile(), sources);
                return FileVisitResult.CONTINUE;
            }
        });

        chatModel.chatStream(PromptTemplate.PROJECT_CODER, data -> {
            data.put("reference", sources.toString());
            data.put("input", "实现Router.java matchHandler方法中的拦截器路由匹配算法。路由的匹配规则类似route方法");
        }, System.out::print);


    }
}
