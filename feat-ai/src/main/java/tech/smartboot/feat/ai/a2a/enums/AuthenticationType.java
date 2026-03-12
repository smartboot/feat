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

/**
 * A2A 认证类型枚举
 *
 * <p>定义了A2A协议中支持的认证机制类型。</p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0 3/12/26
 */
public enum AuthenticationType {
    /**
     * 无需认证
     */
    NONE("none"),

    /**
     * API Key认证
     */
    API_KEY("apiKey"),

    /**
     * OAuth2认证
     */
    OAUTH2("oauth2"),

    /**
     * JWT认证
     */
    JWT("jwt"),

    /**
     * 自定义认证方案
     */
    CUSTOM("custom");

    private final String value;

    AuthenticationType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /**
     * 根据值查找认证类型
     *
     * @param value 认证类型值
     * @return 对应的AuthenticationType枚举，如果未找到则返回null
     */
    public static AuthenticationType fromValue(String value) {
        for (AuthenticationType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        return null;
    }
}
