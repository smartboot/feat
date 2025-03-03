/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.chat.entity;

import java.util.Map;

/**
 * @author 三刀(zhengjunweimail@163.com)
 * @version v1.0.0
 */
public class ToolCall {
    private int index;
    private String id;
    private String type;
    private Map<String, String> function;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, String> getFunction() {
        return function;
    }

    public void setFunction(Map<String, String> function) {
        this.function = function;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    @Override
    public String toString() {
        return "ToolCall{" +
                "index=" + index +
                ", id='" + id + '\'' +
                ", type='" + type + '\'' +
                ", function=" + function +
                '}';
    }
}
