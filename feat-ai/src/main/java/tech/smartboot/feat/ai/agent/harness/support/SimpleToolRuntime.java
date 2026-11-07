/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 */

package tech.smartboot.feat.ai.agent.harness.support;

import tech.smartboot.feat.ai.agent.AgentTool;
import tech.smartboot.feat.ai.agent.harness.ToolInvocation;
import tech.smartboot.feat.ai.agent.harness.ToolResult;
import tech.smartboot.feat.ai.agent.harness.ToolRuntime;
import tech.smartboot.feat.ai.agent.harness.ToolSpec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * 工具运行时参考实现：注册表 + 超时 + 截断 + 熔断。
 * <p>
 * 刻意不在这里做政策判定——政策由 harness 调用 {@code ToolPolicy} 完成，
 * 保持"谁能执行"（runtime）与"准不准执行"（policy）分离，
 * 便于在测试里单独替换其中一侧。
 * </p>
 * <p>
 * 超时用共享的单线程调度器实现（Java 8 没有 {@code orTimeout}），
 * 只负责"到点把 future 标成 TIMEOUT"，不强杀业务线程——真正的资源回收
 * 依赖工具自身的 IO 超时设置。
 * </p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v0.1.0
 */
public final class SimpleToolRuntime implements ToolRuntime {

    private static final ScheduledExecutorService TIMER = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, "feat-ai-tool-timeout");
            thread.setDaemon(true);
            return thread;
        }
    });

    private final Map<String, Entry> entries = new ConcurrentHashMap<String, Entry>();
    private final Set<String> disabled = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public List<ToolSpec> specs() {
        List<ToolSpec> specs = new ArrayList<ToolSpec>();
        for (Entry entry : entries.values()) {
            if (!disabled.contains(entry.spec.getName())) {
                specs.add(entry.spec);
            }
        }
        return specs;
    }

    @Override
    public void register(AgentTool tool, ToolSpec spec) {
        entries.put(spec.getName(), new Entry(tool, spec));
    }

    @Override
    public void disable(String toolName, String reason) {
        disabled.add(toolName);
    }

    @Override
    public boolean isDisabled(String toolName) {
        return disabled.contains(toolName);
    }

    @Override
    public CompletableFuture<ToolResult> invoke(ToolInvocation invocation) {
        Entry entry = entries.get(invocation.getToolName());
        if (entry == null) {
            return CompletableFuture.completedFuture(
                    ToolResult.invalidArgument("未注册的工具：" + invocation.getToolName()));
        }
        if (disabled.contains(invocation.getToolName())) {
            return CompletableFuture.completedFuture(ToolResult.denied("工具已被临时禁用"));
        }
        final long start = System.currentTimeMillis();
        final CompletableFuture<ToolResult> future = new CompletableFuture<ToolResult>();
        final ScheduledFuture<?> timer = TIMER.schedule(new Runnable() {
            @Override
            public void run() {
                future.complete(ToolResult.timeout(entry.spec.getTimeoutMillis()));
            }
        }, entry.spec.getTimeoutMillis(), TimeUnit.MILLISECONDS);

        try {
            entry.tool.execute(invocation.getArguments()).whenComplete((output, error) -> {
                timer.cancel(false);
                long cost = System.currentTimeMillis() - start;
                if (error != null) {
                    future.complete(ToolResult.error(String.valueOf(error), cost));
                } else {
                    future.complete(truncate(entry.spec, invocation, output, cost));
                }
            });
        } catch (Exception e) {
            timer.cancel(false);
            future.complete(ToolResult.error(String.valueOf(e), System.currentTimeMillis() - start));
        }
        return future;
    }

    /**
     * 输出截断。P1 会把溢出的全文落到 {@code artifacts/<sessionId>/<callId>.txt}，
     * 上下文里只留摘要 + 句柄，模型需要细节时再按行读取。
     */
    private ToolResult truncate(ToolSpec spec, ToolInvocation invocation, String output, long cost) {
        String text = output == null ? "" : output;
        int max = spec.getMaxOutputChars();
        if (text.length() <= max) {
            return ToolResult.ok(text, cost, false, null);
        }
        String ref = "tool://" + invocation.getSessionId() + "/" + invocation.getCallId();
        return ToolResult.ok(text.substring(0, max), cost, true, ref);
    }

    public static final class Builder {
        private final SimpleToolRuntime runtime = new SimpleToolRuntime();

        public Builder register(AgentTool tool, ToolSpec spec) {
            runtime.register(tool, spec);
            return this;
        }

        /**
         * 未显式声明治理参数时按工具自描述信息注册，效果等级取默认 READ——
         * 生产环境请显式声明，否则写操作会被当成只读放行。
         */
        public Builder register(AgentTool tool) {
            ToolSpec spec = ToolSpec.builder(tool.getName())
                    .description(tool.getDescription())
                    .parametersSchema(tool.getParametersSchema())
                    .build();
            runtime.register(tool, spec);
            return this;
        }

        public SimpleToolRuntime build() {
            return runtime;
        }
    }

    private static final class Entry {
        private final AgentTool tool;
        private final ToolSpec spec;

        private Entry(AgentTool tool, ToolSpec spec) {
            this.tool = tool;
            this.spec = spec;
        }
    }
}
