/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 */

package tech.smartboot.feat.ai.agent.harness.example;

import tech.smartboot.feat.ai.agent.harness.AgentHarness;
import tech.smartboot.feat.ai.agent.harness.HarnessOptions;
import tech.smartboot.feat.ai.agent.harness.HarnessRequest;
import tech.smartboot.feat.ai.agent.harness.HarnessResult;
import tech.smartboot.feat.ai.agent.harness.ToolPolicy;
import tech.smartboot.feat.ai.agent.harness.ToolSpec;
import tech.smartboot.feat.ai.agent.harness.support.InMemorySessionStore;
import tech.smartboot.feat.ai.agent.harness.support.NativeToolCallLoop;
import tech.smartboot.feat.ai.agent.harness.support.SimpleToolRuntime;
import tech.smartboot.feat.ai.agent.tools.FileOperationTool;

import java.util.concurrent.CompletableFuture;

/**
 * 真实模型示例：一个只读的代码问答代理。
 * <p>
 * 跑之前设置环境变量（DeepSeek 或任意 OpenAI 兼容网关）：
 * <pre>
 * export DEEPSEEK_API_KEY=sk-xxx
 * # 可选
 * export DEEPSEEK_BASE_URL=https://api.deepseek.com
 * export DEEPSEEK_MODEL=deepseek-chat
 * </pre>
 * 未设置 key 时直接退出并提示，不会报错。
 * </p>
 * <p>
 * 这个示例想说明三件在真实项目里最容易踩的事：
 * <ol>
 *     <li><b>工具级 policy 不够细</b>：{@code FileOperationTool} 一个工具里既有读也有写，
 *     只声明 {@code Effect.READ} 会连写操作一起放行，所以这里按 {@code action} 参数二次判定；</li>
 *     <li><b>schema 必须给全</b>：只写工具名和描述，模型会瞎猜参数名——见
 *     {@code NativeToolCallLoop#buildFromSchema}；</li>
 *     <li><b>只读任务的"完成"也需要证据</b>：系统提示里要求给出文件与行号，
 *     让答案可核对，而不是让模型自说自话。</li>
 * </ol>
 * </p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v0.1.0
 */
public final class FileAgentDemo {

    /**
     * 复合工具需要按 action 做二次判定：{@code ToolPolicy.DEFAULT} 只看工具级的
     * {@code Effect}，而 file_operation 同时具备读写能力。
     * <p>
     * 先在 DEFAULT 上过一遍是为了不丢掉它内置的危险参数拦截（rm -rf / drop table 等），
     * 再叠加 action 维度。
     * </p>
     */
    private static final ToolPolicy READ_ONLY_POLICY = (invocation, spec) -> {
        ToolPolicy.Decision base = ToolPolicy.DEFAULT.check(invocation, spec);
        if (base.getKind() == ToolPolicy.Decision.Kind.DENY) {
            return base;
        }
        String action = invocation.getArguments() == null
                ? null : invocation.getArguments().getString("action");
        if (action == null) {
            return ToolPolicy.Decision.deny("缺少 action 参数，无法判定操作类型");
        }
        switch (action) {
            case "list_directory":
            case "read_file":
            case "file_exists":
                return ToolPolicy.Decision.allow();
            case "write_file":
            case "create_directory":
                return ToolPolicy.Decision.ask("写操作：" + action);
            case "delete_file":
                return ToolPolicy.Decision.deny("示例禁止删除文件");
            default:
                return ToolPolicy.Decision.deny("未知操作：" + action);
        }
    };

    public static void main(String[] args) {
        String apiKey = System.getenv("DEEPSEEK_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            System.out.println("未设置 DEEPSEEK_API_KEY，跳过真实模型示例。");
            System.out.println("不连模型也能看 harness 行为：运行 OfflineDemo。");
            return;
        }
        String baseUrl = envOrDefault("DEEPSEEK_BASE_URL", "https://api.deepseek.com");
        String model = envOrDefault("DEEPSEEK_MODEL", "deepseek-chat");

        FileOperationTool fileTool = new FileOperationTool(".");
        ToolSpec fileSpec = ToolSpec.builder(fileTool.getName())
                .description(fileTool.getDescription())
                // 不传 parametersSchema 的话，模型只能看到工具名和一句描述，参数全靠猜
                .parametersSchema(fileTool.getParametersSchema())
                .effect(ToolSpec.Effect.READ)
                .timeoutMillis(10_000)
                .maxOutputChars(12_000)
                .build();

        InMemorySessionStore store = new InMemorySessionStore();
        AgentHarness harness = new AgentHarness(new HarnessOptions()
                .loop(new NativeToolCallLoop(chat -> chat
                        .baseUrl(baseUrl)
                        .model(model)
                        .apiKey(apiKey)))
                .toolRuntime(SimpleToolRuntime.builder()
                        .register(fileTool, fileSpec)
                        .build())
                .toolPolicy(READ_ONLY_POLICY)
                // 生产环境把这个换成"弹窗问人"或"按目录白名单自动放行"；
                // 返回 false 时工具不会执行，拒绝原因会作为观察回喂模型
                .approvalGate((invocation, reason) -> {
                    System.out.println("[审批] " + invocation.getToolName() + " "
                            + invocation.getArguments() + " —— " + reason);
                    return CompletableFuture.completedFuture(Boolean.TRUE);
                })
                .sessionStore(store)
                .systemPrompt("你是代码阅读助手。所有结论必须来自你实际读到的文件内容，"
                        + "并在答案里标注文件路径与行号；没读到就直说没读到，不要凭印象补全。")
                .maxSteps(12)
                .maxTokens(80_000)
                .maxMillis(3 * 60 * 1000L));

        String task = "阅读 feat-ai/src/main/java/tech/smartboot/feat/ai/agent/ReActAgent.java，"
                + "说明它如何从模型输出中解析工具调用，并指出这种解析方式的两个隐患。只读，不要修改任何文件。";

        System.out.println("任务：" + task);
        HarnessResult result = harness.run(HarnessRequest.of("demo-read-only", task));

        System.out.println("\n状态   : " + result.getStatus() + "（verified=" + result.isVerified() + "）");
        System.out.println("步数   : " + result.getSteps() + "，实际执行工具 " + result.getToolCalls() + " 次");
        System.out.println("token  : " + (result.getUsage() == null ? 0 : result.getUsage().getTotalTokens()));
        System.out.println("耗时   : " + result.getDurationMillis() + "ms");
        System.out.println("\n答案：\n" + result.getAnswer());

        if (!result.isSuccess() && result.getStopDetail() != null) {
            System.out.println("\n终止说明：" + result.getStopDetail());
            // 可恢复的终态（MAX_STEPS / BUDGET_EXCEEDED / CANCELLED / DENIED）可以续跑：
            //   harness.resumeAsync("demo-read-only");
            // 注意：UNVERIFIED 不可续跑，重放同样配置只会得到同样结论
        }
        System.out.println("\n事件 " + store.load("demo-read-only").size() + " 条，落在 session demo-read-only");
    }

    private static String envOrDefault(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isEmpty() ? fallback : value;
    }
}
