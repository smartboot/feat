/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 */

package tech.smartboot.feat.ai.agent.harness;

/**
 * 工具政策：放行、拒绝、还是问人。
 * <p>
 * 现成的 {@code Hook#preTool(ToolCaller)} 返回 void，只能旁听不能否决；
 * 治法不应该通过改 Hook 签名（会破坏已有实现）来实现，而是把"决定权"
 * 独立成一个只回答三选一的策略接口。
 * </p>
 * <p>
 * 默认政策按副作用等级分级：
 * READ → 放行；NETWORK → 放行（建议叠加域名白名单）；WRITE / EXEC → 审批。
 * 危险模式（{@code rm -rf}、{@code git push --force}、{@code DROP TABLE}）直接拒绝，
 * 不给模型"问一下就能过"的机会。
 * </p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v0.1.0
 */
public interface ToolPolicy {

    Decision check(ToolInvocation invocation, ToolSpec spec);

    /**
     * 即开即用的默认政策
     */
    ToolPolicy DEFAULT = (invocation, spec) -> {
        if (spec == null) {
            return Decision.deny("未注册的工具：" + invocation.getToolName());
        }
        String args = invocation.getArguments().toJSONString();
        if (DangerousPatterns.hit(args)) {
            return Decision.deny("命中危险操作模式，已拦截");
        }
        if (spec.getApproval() == ToolSpec.Approval.ALWAYS) {
            return Decision.ask("该工具会产生不可逆副作用");
        }
        switch (spec.getEffect()) {
            case WRITE:
            case EXEC:
                return Decision.ask("该工具属于" + spec.getEffect() + "类操作");
            default:
                return Decision.allow();
        }
    };

    /**
     * 政策结论
     */
    final class Decision {
        public enum Kind {
            ALLOW,
            DENY,
            ASK
        }

        private final Kind kind;
        private final String reason;

        private Decision(Kind kind, String reason) {
            this.kind = kind;
            this.reason = reason;
        }

        public static Decision allow() {
            return new Decision(Kind.ALLOW, null);
        }

        public static Decision deny(String reason) {
            return new Decision(Kind.DENY, reason);
        }

        public static Decision ask(String reason) {
            return new Decision(Kind.ASK, reason);
        }

        public Kind getKind() {
            return kind;
        }

        public String getReason() {
            return reason;
        }

        public boolean allowed() {
            return kind == Kind.ALLOW;
        }
    }

    /**
     * 危险参数模式。这里只做"明显不可逆"的兜底，不做完整命令解析——
     * 真正的隔离应落在 Workspace 与容器边界上，而不是正则。
     */
    final class DangerousPatterns {
        private static final String[] PATTERNS = {
                "rm -rf", "rm -fr", "git push --force", "git push -f",
                "drop table", "drop database", "truncate table",
                "shutdown", "mkfs", ":(){:|:&};:"
        };

        private DangerousPatterns() {
        }

        public static boolean hit(String text) {
            if (text == null || text.isEmpty()) {
                return false;
            }
            String lower = text.toLowerCase();
            for (String pattern : PATTERNS) {
                if (lower.contains(pattern)) {
                    return true;
                }
            }
            return false;
        }
    }
}
