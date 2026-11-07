/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 */

package tech.smartboot.feat.ai.agent.harness.support;

import tech.smartboot.feat.ai.agent.harness.AgentEvent;
import tech.smartboot.feat.ai.agent.harness.SessionStore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 内存事件存储：单元测试与单机短期运行的默认实现。
 * <p>
 * 生产环境请替换为 JSONL 落盘实现（P1 交付），否则进程重启即丢失全部执行轨迹，
 * {@code resume} / {@code replay} 都会失效。
 * </p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v0.1.0
 */
public final class InMemorySessionStore implements SessionStore {

    private final Map<String, List<AgentEvent>> sessions = new ConcurrentHashMap<String, List<AgentEvent>>();
    private final AtomicLong seq = new AtomicLong();

    @Override
    public void append(AgentEvent event) {
        event.setSeq(seq.incrementAndGet());
        List<AgentEvent> events = sessions.get(event.getSessionId());
        if (events == null) {
            List<AgentEvent> created = Collections.synchronizedList(new ArrayList<AgentEvent>());
            events = sessions.putIfAbsent(event.getSessionId(), created);
            if (events == null) {
                events = created;
            }
        }
        events.add(event);
    }

    @Override
    public List<AgentEvent> load(String sessionId) {
        List<AgentEvent> events = sessions.get(sessionId);
        if (events == null) {
            return Collections.emptyList();
        }
        synchronized (events) {
            return new ArrayList<AgentEvent>(events);
        }
    }

    @Override
    public void close() {
        sessions.clear();
    }
}
