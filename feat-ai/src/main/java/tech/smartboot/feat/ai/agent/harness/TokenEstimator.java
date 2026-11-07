/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 */

package tech.smartboot.feat.ai.agent.harness;

import com.alibaba.fastjson2.JSON;
import tech.smartboot.feat.ai.chat.entity.Message;

import java.util.List;

/**
 * token 估算器。
 * <p>
 * 上下文窗口是 harness 里最贵的资源，而在发送请求之前必须知道"还塞得下多少"。
 * 各 provider 的精确分词器不可通用，因此这里用**可校准的启发式**：
 * 先按字符构成粗估（ASCII 4 字符≈1 token，非 ASCII 1 字符≈1 token），
 * 再用响应里的 {@code Usage#getPromptTokens()} 反向修正系数。
 * </p>
 * <p>
 * 注意：当前 {@code Message} 基类没有暴露 {@code getContent()}（{@code TextMessage} 为包级私有），
 * 因此默认实现对消息做了 JSON 序列化后估长。后续建议在 {@code Message} 上补一个
 * {@code public String getContent()} 默认实现，可省掉序列化开销。
 * </p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v0.1.0
 */
public interface TokenEstimator {

    /**
     * 估算一段文本的 token 数
     */
    int estimate(String text);

    /**
     * 估算单条消息的 token 数。
     * <p>
     * 注意不能用 {@code String.valueOf(message)}：{@code Message} 没有重写 {@code toString()}，
     * 拿到的是 {@code TextMessage@1a2b3c} 这类身份哈希，估出来的 token 数与内容无关。
     * 必须走 JSON 序列化——顺带把所有字段（tool_calls、tool_call_id）都算进去。
     * </p>
     */
    default int estimate(Message message) {
        if (message == null) {
            return 0;
        }
        return estimate(JSON.toJSONString(message)) + MESSAGE_OVERHEAD;
    }

    /**
     * 估算一组消息的 token 数（含每条消息的角色开销）
     */
    default int estimate(List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return 0;
        }
        int total = 0;
        for (Message message : messages) {
            total += estimate(message);
        }
        return total;
    }

    /**
     * 单条消息的角色/分隔符开销
     */
    int MESSAGE_OVERHEAD = 4;

    /**
     * 用真实 usage 校准估算误差，实现可记录偏差系数
     */
    default void calibrate(int estimatedTokens, int actualPromptTokens) {
    }

    /**
     * 默认启发式估算器
     */
    static TokenEstimator heuristic() {
        return new TokenEstimator() {
            @Override
            public int estimate(String text) {
                if (text == null || text.isEmpty()) {
                    return 0;
                }
                int ascii = 0;
                int wide = 0;
                for (int i = 0; i < text.length(); i++) {
                    if (text.charAt(i) < 0x80) {
                        ascii++;
                    } else {
                        wide++;
                    }
                }
                return (ascii + 3) / 4 + wide;
            }
        };
    }
}
