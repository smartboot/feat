package tech.smartboot.feat.demo.plus.service;

import java.util.Date;

import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Bean;
import tech.smartboot.feat.demo.plus.mapper.ActivityMapper;
import tech.smartboot.feat.demo.plus.mapper.NotificationMapper;
import tech.smartboot.feat.demo.plus.model.ActivityRecord;
import tech.smartboot.feat.demo.plus.model.NotificationRecord;

/**
 * 用户变更同时写入操作动态与站内通知，对应前端 mock 的 recordEvent。
 * <p>
 * operatorId 是执行这次操作的人（creator / editor），userId 是被操作的对象，两者语义不同。
 */
@Bean
public class ActivityRecorder {

    @Autowired
    private ActivityMapper activityMapper;

    @Autowired
    private NotificationMapper notificationMapper;

    public void record(String kind, String title, String description, Long userId, long operatorId) {
        Date now = new Date();

        ActivityRecord activity = new ActivityRecord();
        activity.setKind(kind);
        activity.setTitle("管理员" + title);
        activity.setDescription(description);
        activity.setUserId(userId);
        activity.setIsDeleted(0);
        activity.setCreatedTime(now);
        activity.setEditTime(now);
        activity.setCreator(operatorId);
        activity.setEditor(operatorId);
        activityMapper.insert(activity);

        NotificationRecord notification = new NotificationRecord();
        notification.setTitle(title);
        notification.setDescription(description);
        // 通知发给「被操作的人」。user_id 为 NULL 在查询里被当作广播，
        // 所以没有明确接收人（例如删除账号）时干脆不发通知，避免悄悄退化成全员广播。
        if (userId != null) {
            notification.setUserId(userId);
            notification.setIsDeleted(0);
            notification.setCreatedTime(now);
            notification.setEditTime(now);
            notification.setCreator(operatorId);
            notification.setEditor(operatorId);
            notificationMapper.insert(notification);
        }
    }
}
