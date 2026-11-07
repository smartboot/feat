/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 */

package tech.smartboot.feat.ai.agent.harness;

import java.util.concurrent.CompletableFuture;

/**
 * 结果验证器：在 {@link Step.Type#FINAL} 落地之前插一道机械检查。
 * <p>
 * 为什么需要它：模型的自我评价不可信。直接把"模型说完成了"当成成功，
 * 等于把验收标准交给了被验收方。业界的做法是把生成与评审拆成两个角色
 * （GAN 式评审 agent），工程上更轻的版本就是这里——**用可判定的机械信号**
 * （测试退出码、lint 结果、编译产物是否存在）替代主观评价。
 * </p>
 * <p>
 * 与 {@link ToolRuntime} 的区别：验证器不改变任务进度，只对"结论"投一次票；
 * 它没有审批、没有权限，因为它默认只读。
 * </p>
 * <p>
 * 实现约束：
 * <ol>
 *     <li>必须快速返回，它是 FINAL 路径上的同步阻塞点，慢验证等于慢任务；</li>
 *     <li>抛出的异常会被 harness 转成 {@link VerifyResult#skip} 而非 FAIL；</li>
 *     <li>反馈文本会被原样回灌给模型，因此要写成"模型能据此修正"的形式，
 *         不要写成给人看的报告。</li>
 * </ol>
 * </p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v0.1.0
 */
public interface Verifier {

    /**
     * 未配置验证器：一律放行，保持与旧行为一致
     */
    Verifier NONE = (context, answer) -> CompletableFuture.completedFuture(
            VerifyResult.skip("none", "未配置验证器"));

    /**
     * 验证候选答案
     *
     * @param context 当前步进上下文（可读原始历史、工具清单、步号）
     * @param answer  模型给出的最终答案
     * @return 验证结论；不得返回 null
     */
    CompletableFuture<VerifyResult> verify(StepContext context, String answer);
}
