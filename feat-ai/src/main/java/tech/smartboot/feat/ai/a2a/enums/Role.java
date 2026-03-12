/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.a2a.enums;

import com.alibaba.fastjson2.annotation.JSONField;

/**
 * A2A 角色枚举
 *
 * <p>定义了A2A协议中消息发送者的角色类型。</p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0 3/12/26
 */
public enum Role {
    /**
     * 用户角色
     */
    USER("user"),

    /**
     * 智能体角色
     */
    AGENT("agent");

    @JSONField(value = true)
    private final String value;

    Role(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /**
     * 根据值查找角色
     *
     * @param value 角色值
     * @return 对应的Role枚举，如果未找到则返回null
     */
    public static Role fromValue(String value) {
        for (Role role : values()) {
            if (role.value.equals(value)) {
                return role;
            }
        }
        return null;
    }
}
