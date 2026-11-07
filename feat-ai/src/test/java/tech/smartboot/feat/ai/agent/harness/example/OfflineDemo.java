/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 */

package tech.smartboot.feat.ai.agent.harness.example;

import com.alibaba.fastjson2.JSONObject;
import tech.smartboot.feat.ai.agent.AgentTool;
import tech.smartboot.feat.ai.agent.harness.AgentEvent;
import tech.smartboot.feat.ai.agent.harness.AgentHarness;
import tech.smartboot.feat.ai.agent.harness.ApprovalGate;
import tech.smartboot.feat.ai.agent.harness.HarnessOptions;
import tech.smartboot.feat.ai.agent.harness.HarnessRequest;
import tech.smartboot.feat.ai.agent.harness.HarnessResult;
import tech.smartboot.feat.ai.agent.harness.SessionStore;
import tech.smartboot.feat.ai.agent.harness.Step;
import tech.smartboot.feat.ai.agent.harness.StepContext;
import tech.smartboot.feat.ai.agent.harness.ToolSpec;
import tech.smartboot.feat.ai.agent.harness.Verifier;
import tech.smartboot.feat.ai.agent.harness.VerifyResult;
import tech.smartboot.feat.ai.agent.harness.support.InMemorySessionStore;
import tech.smartboot.feat.ai.agent.harness.support.SimpleToolRuntime;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 离线示例：不连模型也能看清 harness 这一侧在做什么。
 * <p>
 * 两个场景分别对应两个最容易出事的地方：
 * <ol>
 *     <li><b>写操作被审批拒绝</b>——工具不会执行，拒绝原因作为观察回喂模型，让它换路；</li>
 *     <li><b>答案没通过机械验收</b>——验证器读真实文件、给出具体缺什么，模型据此重做。</li>
 * </ol>
 * 模型被 {@link ScriptedLoop} 替换成固定脚本，因为这里要验证的是 harness 的治理行为，
 * 而不是模型有多聪明。接真实模型的写法见 {@link FileAgentDemo}。
 * </p>
 * <p>
 * 直接运行 {@code main}，无需任何配置。
 * </p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v0.1.0
 */
public final class OfflineDemo {

    /**
     * 示例工作目录。放 target 下，随构建清理，不会污染源码树
     */
    private static final Path WORKSPACE = Paths.get("target", "harness-demo");

    private static final String TODO_TWO = "- 补齐订单模块的 NPE 单测\n- 把校验逻辑下沉到 domain 层\n";
    private static final String TODO_THREE = TODO_TWO + "- 把结论回填到 README\n";

    public static void main(String[] args) throws IOException {
        Files.createDirectories(WORKSPACE);
        Files.deleteIfExists(WORKSPACE.resolve("notes.md"));

        scenarioDenied();
        scenarioVerifyRetry();
    }

    // ==================== 场景一：写操作被审批拒绝 ====================

    /**
     * 关注输出里的 <code>POLICY_DECIDED=ASK</code> 与 <code>TOOL_COMPLETED=DENIED</code>。
     * <p>
     * 审批门返回 false 时，工具**没有被执行**（文件不存在），拒绝原因被包装成观察回喂模型，
     * 模型据此换了一条只读路径给出结论。若模型连续 3 次撞同一道门，
     * harness 会以 {@code RunStatus.DENIED} 收尾，而不是一直烧预算。
     * </p>
     */
    private static void scenarioDenied() throws IOException {
        System.out.println("\n================= 场景一：写操作被审批拒绝 =================");

        InMemorySessionStore store = new InMemorySessionStore();
        List<Step> script = Arrays.asList(
                Step.toolCall("call-1", "write_file",
                        writeArgs("notes.md", TODO_TWO), "先落盘", null),
                Step.finalAnswer("写入未获批准",
                        "写入未获批准，我把待办直接整理在回答里：\n" + TODO_THREE, null));

        AgentHarness harness = new AgentHarness(new HarnessOptions()
                .loop(new ScriptedLoop("scripted", script))
                .toolRuntime(runtime())
                .sessionStore(store)
                // 演示用：一律不批。生产环境通常是"弹窗问人"或"只放行白名单目录"
                .approvalGate(ApprovalGate.DENY_ALL)
                .maxSteps(10));

        HarnessResult result = harness.run(HarnessRequest.of("demo-denied", "把待办整理进 notes.md"));
        printResult(result);

        System.out.println("  notes.md 是否被创建：" + Files.exists(WORKSPACE.resolve("notes.md"))
                + "（期望 false —— 审批拒绝时工具根本不会执行）");
        dumpEvents(store, "demo-denied");
    }

    // ==================== 场景二：验收不通过 → 回灌 → 通过 ====================

    /**
     * 关注输出里的两次 <code>VERIFY_DECIDED</code>：第一次 FAIL、第二次 PASS。
     * <p>
     * 验证器不是"问模型做得对不对"，而是读真实文件数条目——机械信号。
     * 第一次写完只有 2 条，验证器把"缺第三条"写进反馈，harness 原样回灌，
     * 模型（脚本）据此补写，第二次通过。
     * </p>
     */
    private static void scenarioVerifyRetry() throws IOException {
        System.out.println("\n========== 场景二：验收不通过 → 回灌重做 → 通过 ==========");
        Files.deleteIfExists(WORKSPACE.resolve("notes.md"));

        InMemorySessionStore store = new InMemorySessionStore();
        List<Step> script = Arrays.asList(
                Step.toolCall("call-1", "write_file",
                        writeArgs("notes.md", TODO_TWO), "先写前两条", null),
                Step.finalAnswer("已写入", "已写入 notes.md（2 条待办）", null),
                // 上面这句被验证器否决后，harness 回灌反馈并继续，脚本接着走这两步
                Step.toolCall("call-2", "write_file",
                        writeArgs("notes.md", TODO_THREE), "补上第三条", null),
                Step.finalAnswer("已补全", "已写入 notes.md（3 条待办，含回填 README）", null));

        AgentHarness harness = new AgentHarness(new HarnessOptions()
                .loop(new ScriptedLoop("scripted", script))
                .toolRuntime(runtime())
                .sessionStore(store)
                .approvalGate(ApprovalGate.ALLOW_ALL)      // 演示用：自动批准
                .verifier(new TodoFileVerifier(WORKSPACE.resolve("notes.md")))
                .maxVerifyAttempts(2)
                .maxSteps(10));

        HarnessResult result = harness.run(HarnessRequest.of("demo-verify", "把三条待办写进 notes.md"));
        printResult(result);
        dumpEvents(store, "demo-verify");
    }

    // ==================== 装配 ====================

    private static SimpleToolRuntime runtime() {
        String schema = "{\"type\":\"object\",\"properties\":{"
                + "\"path\":{\"type\":\"string\",\"description\":\"相对工作目录的文件路径\"},"
                + "\"content\":{\"type\":\"string\",\"description\":\"要写入的完整文本\"}},"
                + "\"required\":[\"path\",\"content\"]}";
        ToolSpec writeSpec = ToolSpec.builder("write_file")
                .description("把文本写入指定文件，覆盖原内容")
                .parametersSchema(schema)
                // 关键的一行：声明为 WRITE，默认政策才会要求审批
                .effect(ToolSpec.Effect.WRITE)
                .approval(ToolSpec.Approval.ALWAYS)
                .timeoutMillis(5_000)
                .maxOutputChars(2_000)
                .build();
        return SimpleToolRuntime.builder()
                .register(new WriteFileTool(WORKSPACE), writeSpec)
                .build();
    }

    private static JSONObject writeArgs(String path, String content) {
        JSONObject args = new JSONObject();
        args.put("path", path);
        args.put("content", content);
        return args;
    }

    // ==================== 工具与验证器 ====================

    /**
     * 极简文件写入工具。真实项目请用 {@code FileOperationTool}，
     * 它对路径逃逸、目录穿越有额外约束。
     */
    private static final class WriteFileTool implements AgentTool {

        private final Path root;

        private WriteFileTool(Path root) {
            this.root = root;
        }

        @Override
        public CompletableFuture<String> execute(JSONObject parameters) {
            try {
                Files.createDirectories(root);
                Path file = root.resolve(parameters.getString("path"));
                byte[] bytes = parameters.getString("content").getBytes(StandardCharsets.UTF_8);
                Files.write(file, bytes);
                return CompletableFuture.completedFuture(
                        "已写入 " + file.getFileName() + "，" + countLines(bytes) + " 行");
            } catch (Exception e) {
                // 工具内部失败返回文本，交给模型判断；不要抛异常打断整个 run
                return CompletableFuture.completedFuture("写入失败：" + e.getMessage());
            }
        }

        @Override
        public String getName() {
            return "write_file";
        }

        @Override
        public String getDescription() {
            return "把文本写入指定文件，覆盖原内容";
        }

        @Override
        public String getParametersSchema() {
            return "{\"type\":\"object\",\"properties\":{"
                    + "\"path\":{\"type\":\"string\"},\"content\":{\"type\":\"string\"}},"
                    + "\"required\":[\"path\",\"content\"]}";
        }

        private static int countLines(byte[] bytes) {
            int lines = 0;
            for (byte b : bytes) {
                if (b == '\n') {
                    lines++;
                }
            }
            return lines;
        }
    }

    /**
     * 机械验证：数文件里以 "- " 开头的条目，不足 3 条即否决。
     * <p>
     * 反馈必须写成"模型能据此改"的形式（差什么、当前是什么），
     * 而不是"验证失败"。前者模型能修，后者只会让它换个说法再交一次。
     * </p>
     */
    private static final class TodoFileVerifier implements Verifier {

        private final Path file;

        private TodoFileVerifier(Path file) {
            this.file = file;
        }

        @Override
        public CompletableFuture<VerifyResult> verify(StepContext context, String answer) {
            long start = System.currentTimeMillis();
            List<String> todos = new ArrayList<String>();
            if (Files.exists(file)) {
                try {
                    for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                        if (line.startsWith("- ")) {
                            todos.add(line);
                        }
                    }
                } catch (IOException e) {
                    // 环境问题按弃权处理：不能因为读不到文件就判答案错
                    return CompletableFuture.completedFuture(
                            VerifyResult.skip("todo-file", "无法读取文件：" + e.getMessage()));
                }
            }
            long cost = System.currentTimeMillis() - start;
            if (todos.size() >= 3) {
                return CompletableFuture.completedFuture(
                        VerifyResult.pass("todo-file", "文件含 " + todos.size() + " 条待办", cost));
            }
            return CompletableFuture.completedFuture(VerifyResult.fail("todo-file",
                    "notes.md 实际只有 " + todos.size() + " 条待办（按 \"- \" 开头计），"
                            + "缺第 3 条：把结论回填到 README。请补全后重写整个文件。", cost));
        }
    }

    // ==================== 输出 ====================

    private static void printResult(HarnessResult result) {
        System.out.println("  终态     : " + result.getStatus()
                + "   verified=" + result.isVerified()
                + "   resumable=" + result.isResumable());
        // toolCalls 只统计真正执行的调用：被政策/审批拦下的不会到达 runtime，因而不计数
        System.out.println("  步数     : " + result.getSteps()
                + "，实际执行工具 " + result.getToolCalls()
                + " 次，耗时 " + result.getDurationMillis() + "ms");
        System.out.println("  答案     : " + oneLine(result.getAnswer()));
        if (result.getStopDetail() != null) {
            System.out.println("  终止说明 : " + result.getStopDetail());
        }
    }

    private static void dumpEvents(SessionStore store, String sessionId) {
        List<AgentEvent> events = store.load(sessionId);
        int prefixBreaks = 0;
        System.out.println("  事件流（" + events.size() + " 条）：");
        for (AgentEvent event : events) {
            if (event.getType() == AgentEvent.Type.CONTEXT_ASSEMBLED) {
                prefixBreaks++;
            }
            System.out.println("    " + pad(event.getType().name(), 18)
                    + "step=" + event.getStepIndex() + "  " + brief(event.getPayload()));
        }
        System.out.println("  前缀指纹变化 " + prefixBreaks + " 次。"
                + "注意：本示例的会话很短，窗口不足指纹预算（4000 字符），"
                + "正常追加也会被计入；只有长会话里这个数字才等于缓存真实失效次数");
    }

    private static String brief(JSONObject payload) {
        if (payload == null || payload.isEmpty()) {
            return "";
        }
        String text = payload.toJSONString();
        return text.length() <= 150 ? text : text.substring(0, 150) + "…";
    }

    private static String oneLine(String text) {
        if (text == null) {
            return "null";
        }
        String oneLine = text.replace("\n", " / ");
        return oneLine.length() <= 110 ? oneLine : oneLine.substring(0, 110) + "…";
    }

    private static String pad(String text, int width) {
        StringBuilder sb = new StringBuilder(text);
        while (sb.length() < width) {
            sb.append(' ');
        }
        return sb.toString();
    }
}
