/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.a2a;

import tech.smartboot.feat.core.common.exception.FeatException;

/**
 * A2A (Agent-to-Agent) 协议异常类
 *
 * <p>当A2A通信过程中发生错误时抛出此异常，包含详细的错误信息。</p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0 3/12/26
 */
public class A2AException extends FeatException {

    /**
     * 构造一个带有详细错误消息的 A2AException
     *
     * @param message 错误消息
     */
    public A2AException(String message) {
        super(message);
    }

    /**
     * 构造一个带有详细错误消息和原始异常的 A2AException
     *
     * @param message 错误消息
     * @param cause   原始异常
     */
    public A2AException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 构造一个带有原始异常的 A2AException
     *
     * @param cause 原始异常
     */
    public A2AException(Throwable cause) {
        super(cause);
    }
}
