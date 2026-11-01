package tech.smartboot.feat.demo.plus.dashboard.service;

import java.util.Date;

import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Bean;
import tech.smartboot.feat.demo.plus.dashboard.mapper.ActivityMapper;
import tech.smartboot.feat.demo.plus.dashboard.mapper.NotificationMapper;
import tech.smartboot.feat.demo.plus.dashboard.model.ActivityRecord;
import tech.smartboot.feat.demo.plus.dashboard.model.NotificationRecord;

/**
 * 用户变更同时写入操作动态与站内通知，对应前端 mock 的 recordEvent。
 */
@Bean
public class ActivityRecorder {

    @Autowired
    private ActivityMapper activityMapper;

    @Autowired
    private NotificationMapper notificationMapper;

    public void record(String kind, String title, String description, Long userId) {
        Date now = new Date();

        ActivityRecord activity = new ActivityRecord();
        activity.setKind(kind);
        activity.setTitle("管理员" + title);
        activity.setDescription(description);
        activity.setUserId(userId);
        activity.setCreatedAt(now);
        activityMapper.insert(activity);

        NotificationRecord notification = new NotificationRecord();
        notification.setTitle(title);
        notification.setDescription(description);
        notification.setUserId(userId);
        notification.setReadFlag(0);
        notification.setCreatedAt(now);
        notificationMapper.insert(notification);
    }
}
