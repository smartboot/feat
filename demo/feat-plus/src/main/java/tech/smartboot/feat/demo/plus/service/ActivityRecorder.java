package tech.smartboot.feat.demo.plus.service;

import java.util.Date;

import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Bean;
import tech.smartboot.feat.demo.plus.mapper.ActivityMapper;
import tech.smartboot.feat.demo.plus.mapper.NotificationMapper;
import tech.smartboot.feat.demo.plus.model.ActivityRecord;
import tech.smartboot.feat.demo.plus.model.NotificationRecord;
import tech.smartboot.feat.demo.plus.tx.TxTemplate;

/**
 * 用户变更同时写入操作动态与站内通知，对应前端 mock 的 recordEvent。
 * <p>
 * operatorId 是执行这次操作的人（creator / editor），userId 是被操作的对象，两者语义不同。
 * <p>
 * <b>这个方法自己就是一个事务。</b>两条 insert 必须同生共死，否则会出现「动态里说改了，通知栏里没有」，
 * 或者反过来——用户被删了，通知还挂在他 id 上。写入走 {@link TxTemplate}：被业务事务包住时自动加入外层
 * （与外层同生共死），独立调用时自己开一个事务。这两种情况都不需要调用方关心。
 * <p>
 * 关于「失败的操作也要留痕」：那不属于这里。审计记的是「谁改了什么」，失败的操作没有改掉任何东西，
 * 记进去只会让审计说谎；真正的失败留痕在访问日志。
 */
@Bean
public class ActivityRecorder {

    @Autowired
    private TxTemplate txTemplate;

    public void record(String kind, String title, String description, Long userId, long operatorId) {
        txTemplate.run(session -> {
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
            session.getMapper(ActivityMapper.class).insert(activity);

            // 通知发给「被操作的人」。user_id 为 NULL 在查询里被当作广播，
            // 所以没有明确接收人（例如删除账号）时干脆不发通知，避免悄悄退化成全员广播。
            if (userId == null) {
                return;
            }
            NotificationRecord notification = new NotificationRecord();
            notification.setTitle(title);
            notification.setDescription(description);
            notification.setUserId(userId);
            notification.setIsDeleted(0);
            notification.setCreatedTime(now);
            notification.setEditTime(now);
            notification.setCreator(operatorId);
            notification.setEditor(operatorId);
            session.getMapper(NotificationMapper.class).insert(notification);
        });
    }
}
