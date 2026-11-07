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
 * 上下文前缀指纹。
 * <p>
 * 用途只有一个：**把"缓存有没有命中"变成可以观测的数字**。
 * provider 侧的 KV-cache 按前缀匹配，只要前缀字节不变就命中；
 * 因此"前缀指纹在相邻两步之间是否相同"就等于"缓存是否被打断"，
 * 不需要等账单出来才知道。
 * </p>
 * <p>
 * 计算方式是 FNV-1a 64 位哈希，取窗口**开头固定字符预算**的序列化结果
 * （默认 4000 字符）。这里必须是"字符预算"而不是"前 N 条消息"：
 * 消息条数会随追加而变，条数口径会把"正常的尾部追加"误判成前缀变化，
 * 测出来的打断次数比真实的高；字符口径则天然满足"追加不改变前缀"。
 * </p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v0.1.0
 */
public final class ContextSignature {

    private static final long FNV_OFFSET = 0xcbf29ce484222325L;
    private static final long FNV_PRIME = 0x100000001b3L;

    /**
     * 默认参与指纹计算的字符预算
     */
    public static final int DEFAULT_CHARS = 4_000;

    private ContextSignature() {
    }

    public static String of(List<Message> messages) {
        return of(messages, DEFAULT_CHARS);
    }

    public static String of(List<Message> messages, int maxChars) {
        if (messages == null || messages.isEmpty()) {
            return "empty";
        }
        long hash = FNV_OFFSET;
        int consumed = 0;
        for (Message message : messages) {
            String text = JSON.toJSONString(message);
            for (int i = 0; i < text.length() && consumed < maxChars; i++, consumed++) {
                hash ^= text.charAt(i);
                hash *= FNV_PRIME;
            }
            // 消息边界也参与哈希，避免 ["ab","c"] 与 ["a","bc"] 撞成同一个指纹
            hash ^= 0x1f;
            hash *= FNV_PRIME;
            if (consumed >= maxChars) {
                break;
            }
        }
        return Long.toHexString(hash);
    }
}
