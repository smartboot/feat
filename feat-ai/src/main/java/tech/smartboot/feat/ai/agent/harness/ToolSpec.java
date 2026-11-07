/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 */

package tech.smartboot.feat.ai.agent.harness;

import tech.smartboot.feat.ai.chat.entity.Tool;

/**
 * 工具治理元信息：注册时声明一次，之后所有调用都受它约束。
 * <p>
 * feat-ai 现有的 {@code AgentTool} 只描述了"能做什么"（name/description/schema），
 * 没有描述"允许怎么做"。副作用等级、审批要求、超时、输出上限这四项，
 * 恰恰是生产环境出事的地方，因此从工具接口里拆出来单独声明，
 * 避免让每个工具实现都去关心治理策略。
 * </p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v0.1.0
 */
public final class ToolSpec {

    /**
     * 副作用等级
     */
    public enum Effect {
        /**
         * 只读，可安全重放
         */
        READ,
        /**
         * 写文件 / 改数据，不可逆
         */
        WRITE,
        /**
         * 执行命令，等价于给模型一个 shell
         */
        EXEC,
        /**
         * 出网请求，需防 SSRF
         */
        NETWORK
    }

    /**
     * 审批要求
     */
    public enum Approval {
        /**
         * 不审批，按 {@link Effect} 的默认政策执行
         */
        INHERIT,
        /**
         * 每次调用都要审批
         */
        ALWAYS,
        /**
         * 同一会话内首次审批通过后放行
         */
        ONCE_PER_SESSION
    }

    private final String name;
    private final String description;
    private final String parametersSchema;
    private final Tool nativeTool;
    private final Effect effect;
    private final Approval approval;
    private final long timeoutMillis;
    private final int maxOutputChars;
    private final int maxRetries;
    private final boolean idempotent;

    private ToolSpec(Builder builder) {
        this.name = builder.name;
        this.description = builder.description;
        this.parametersSchema = builder.parametersSchema;
        this.nativeTool = builder.nativeTool;
        this.effect = builder.effect;
        this.approval = builder.approval;
        this.timeoutMillis = builder.timeoutMillis;
        this.maxOutputChars = builder.maxOutputChars;
        this.maxRetries = builder.maxRetries;
        this.idempotent = builder.idempotent;
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getParametersSchema() {
        return parametersSchema;
    }

    /**
     * 原生 tool_calls 所需的 {@link Tool} 描述；为空表示该工具只能走文本协议
     */
    public Tool getNativeTool() {
        return nativeTool;
    }

    public Effect getEffect() {
        return effect;
    }

    public Approval getApproval() {
        return approval;
    }

    public long getTimeoutMillis() {
        return timeoutMillis;
    }

    public int getMaxOutputChars() {
        return maxOutputChars;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    /**
     * 只有幂等工具才允许在超时/网络错误后自动重试
     */
    public boolean isIdempotent() {
        return idempotent;
    }

    public static final class Builder {
        private final String name;
        private String description;
        private String parametersSchema;
        private Tool nativeTool;
        private Effect effect = Effect.READ;
        private Approval approval = Approval.INHERIT;
        private long timeoutMillis = 30_000L;
        private int maxOutputChars = 8_000;
        private int maxRetries = 0;
        private boolean idempotent = true;

        private Builder(String name) {
            this.name = name;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder parametersSchema(String parametersSchema) {
            this.parametersSchema = parametersSchema;
            return this;
        }

        public Builder nativeTool(Tool nativeTool) {
            this.nativeTool = nativeTool;
            return this;
        }

        public Builder effect(Effect effect) {
            this.effect = effect;
            return this;
        }

        public Builder approval(Approval approval) {
            this.approval = approval;
            return this;
        }

        public Builder timeoutMillis(long timeoutMillis) {
            this.timeoutMillis = Math.max(1L, timeoutMillis);
            // 写/执行类操作不重试，避免重复产生副作用
            return this;
        }

        public Builder maxOutputChars(int maxOutputChars) {
            this.maxOutputChars = Math.max(64, maxOutputChars);
            return this;
        }

        public Builder maxRetries(int maxRetries) {
            this.maxRetries = Math.max(0, maxRetries);
            return this;
        }

        public Builder idempotent(boolean idempotent) {
            this.idempotent = idempotent;
            return this;
        }

        public ToolSpec build() {
            return new ToolSpec(this);
        }
    }
}
