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
 * A2A 任务状态枚举
 *
 * <p>定义了A2A协议中任务的各种状态。</p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0 3/12/26
 */
public enum TaskState {
    /**
     * 任务已提交，正在等待处理
     */
    SUBMITTED("submitted"),

    /**
     * 任务正在处理中
     */
    WORKING("working"),

    /**
     * 任务需要用户输入才能继续
     */
    INPUT_REQUIRED("input_required"),

    /**
     * 任务已完成
     */
    COMPLETED("completed"),

    /**
     * 任务已取消
     */
    CANCELED("canceled"),

    /**
     * 任务失败
     */
    FAILED("failed"),

    /**
     * 任务未知状态
     */
    UNKNOWN("unknown");

    @JSONField(value = true)
    private final String value;

    TaskState(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /**
     * 检查任务是否处于最终状态（已完成、失败或取消）
     *
     * @return 如果任务已完成则返回true
     */
    public boolean isFinal() {
        return this == COMPLETED || this == FAILED || this == CANCELED;
    }

    /**
     * 根据值查找任务状态
     *
     * @param value 状态值
     * @return 对应的TaskState枚举，如果未找到则返回null
     */
    public static TaskState fromValue(String value) {
        for (TaskState state : values()) {
            if (state.value.equals(value)) {
                return state;
            }
        }
        return null;
    }
}
