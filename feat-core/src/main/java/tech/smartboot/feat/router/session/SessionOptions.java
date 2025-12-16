/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.router.session;

/**
 * 会话配置选项类，用于配置会话相关参数
 * <p>
 * SessionOptions提供了会话管理的相关配置选项，目前主要包含会话最大存活时间的配置。
 * 通过此类可以统一管理会话的默认行为和超时设置。
 * </p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0 3/3/25
 */
public class SessionOptions {
    /**
     * 会话的最大存活时间, 单位：秒
     * <p>
     * 表示会话在无活动状态下的最大存活时间，超过此时间未访问的会话将被自动清理。
     * 默认值为30分钟（1800秒）。
     * </p>
     */
    private int maxAge = 30 * 60;

    /**
     * 获取会话最大存活时间
     *
     * @return 会话最大存活时间（秒）
     */
    public int getMaxAge() {
        return maxAge;
    }

    /**
     * 设置会话最大存活时间
     *
     * @param maxAge 会话最大存活时间（秒）
     * @return 当前会话配置选项实例，支持链式调用
     */
    public SessionOptions setMaxAge(int maxAge) {
        this.maxAge = maxAge;
        return this;
    }
}