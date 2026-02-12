/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.core.server.upgrade.sse;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
class SseEventBuilderImpl implements SseEventBuilder {

    private final StringBuilder sb = new StringBuilder();

    @Override
    public SseEventBuilder id(String id) {
        sb.append("id:").append(id).append('\n');
        return this;
    }

    @Override
    public SseEventBuilder name(String name) {
        sb.append("event:").append(name).append('\n');
        return this;
    }

    @Override
    public SseEventBuilder reconnectTime(long reconnectTimeMillis) {
        sb.append("retry:").append(reconnectTimeMillis).append('\n');
        return this;
    }

    @Override
    public SseEventBuilder comment(String comment) {
        sb.append(':').append(comment).append('\n');
        return this;
    }

    @Override
    public SseEventBuilder data(String data) {
        sb.append("data:");
        for (int i = 0; i < data.length(); i++) {
            char c = data.charAt(i);
            if (c == '\n') {
                sb.append("\\n");
            } else {
                sb.append(c);
            }
        }
        sb.append('\n');
        return this;
    }

    @Override
    public String build() {
        return sb.append('\n').toString();
    }

}