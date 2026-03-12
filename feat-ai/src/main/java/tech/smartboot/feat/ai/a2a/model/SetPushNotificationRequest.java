/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.a2a.model;

/**
 * A2A 设置推送通知请求类
 *
 * <p>用于设置任务推送通知的请求参数。</p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0 3/12/26
 */
public class SetPushNotificationRequest {
    /**
     * 任务ID
     */
    private String id;

    /**
     * 推送通知配置
     */
    private PushNotificationConfig pushNotification;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public PushNotificationConfig getPushNotification() {
        return pushNotification;
    }

    public void setPushNotification(PushNotificationConfig pushNotification) {
        this.pushNotification = pushNotification;
    }
}
