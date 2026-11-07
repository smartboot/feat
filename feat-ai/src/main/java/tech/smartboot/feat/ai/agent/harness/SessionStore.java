/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 */

package tech.smartboot.feat.ai.agent.harness;

import java.util.List;

/**
 * 事件存储：harness 的状态外置点。
 * <p>
 * 只做 append 与按会话读取，不做查询语言、不做分页——需要分析时
 * 把 JSONL 导到任何工具里都比在框架里造一套查询 API 划算。
 * </p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v0.1.0
 */
public interface SessionStore {

    /**
     * 追加一条事件。实现需保证同一会话内 seq 单调递增。
     */
    void append(AgentEvent event);

    /**
     * 读取某会话的全部事件，按 seq 升序
     */
    List<AgentEvent> load(String sessionId);

    default void close() {
    }
}
