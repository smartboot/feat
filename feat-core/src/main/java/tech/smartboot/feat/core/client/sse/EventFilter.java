/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.core.client.sse;

/**
 * SSE事件过滤器接口
 * 
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
@FunctionalInterface
public interface EventFilter {
    
    /**
     * 检查事件是否应该被处理
     * 
     * @param event SSE事件对象
     * @return 如果事件应该被处理返回true，否则返回false
     */
    boolean accept(SseEvent event);
    
    /**
     * 创建一个基于事件类型的过滤器
     * 
     * @param eventType 要过滤的事件类型
     * @return 事件过滤器
     */
    static EventFilter byType(String eventType) {
        return event -> eventType.equals(event.getType());
    }
    
    /**
     * 创建一个接受所有事件的过滤器
     * 
     * @return 接受所有事件的过滤器
     */
    static EventFilter acceptAll() {
        return event -> true;
    }
    
    /**
     * 创建一个拒绝所有事件的过滤器
     * 
     * @return 拒绝所有事件的过滤器
     */
    static EventFilter rejectAll() {
        return event -> false;
    }
}