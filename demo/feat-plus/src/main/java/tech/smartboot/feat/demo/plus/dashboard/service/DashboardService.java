package tech.smartboot.feat.demo.plus.dashboard.service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Bean;
import tech.smartboot.feat.demo.plus.dashboard.dto.ActivityItemResponse;
import tech.smartboot.feat.demo.plus.dashboard.dto.DashboardSummaryResponse;
import tech.smartboot.feat.demo.plus.dashboard.dto.NotificationItemResponse;
import tech.smartboot.feat.demo.plus.dashboard.dto.TrendPoint;
import tech.smartboot.feat.demo.plus.dashboard.mapper.ActivityMapper;
import tech.smartboot.feat.demo.plus.dashboard.mapper.LoginStatMapper;
import tech.smartboot.feat.demo.plus.dashboard.mapper.NotificationMapper;
import tech.smartboot.feat.demo.plus.dashboard.model.ActivityRecord;
import tech.smartboot.feat.demo.plus.dashboard.model.DailyLogin;
import tech.smartboot.feat.demo.plus.dashboard.model.NotificationRecord;
import tech.smartboot.feat.demo.plus.seed.DataInitializer;
import tech.smartboot.feat.demo.plus.user.mapper.UserMapper;

/**
 * 仪表盘与通知。
 */
@Bean
public class DashboardService {

    private static final int TREND_SHORT = 7;
    private static final int TREND_LONG = 30;
    private static final int RECENT_ACTIVITY = 7;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ActivityMapper activityMapper;

    @Autowired
    private NotificationMapper notificationMapper;

    @Autowired
    private LoginStatMapper loginStatMapper;

    @Autowired
    private DataInitializer dataInitializer;

    public DashboardSummaryResponse summary() {
        long total = userMapper.countAll();
        long enabled = userMapper.countPage(null, 1, null, null);
        long disabled = userMapper.countPage(null, 0, null, null);

        List<ActivityRecord> recent = activityMapper.selectRecent(RECENT_ACTIVITY);
        List<ActivityItemResponse> activities = new ArrayList<>(recent.size());
        for (ActivityRecord record : recent) {
            activities.add(new ActivityItemResponse(record));
        }

        return new DashboardSummaryResponse(total, enabled, disabled, notificationMapper.countUnread(),
                buildTrend(TREND_SHORT), buildTrend(TREND_LONG), activities);
    }

    public List<NotificationItemResponse> notifications() {
        List<NotificationRecord> rows = notificationMapper.selectAll();
        List<NotificationItemResponse> items = new ArrayList<>(rows.size());
        for (NotificationRecord row : rows) {
            items.add(new NotificationItemResponse(row));
        }
        return items;
    }

    public void markRead(long id) {
        notificationMapper.markRead(id);
    }

    public void markAllRead() {
        notificationMapper.markAllRead();
    }

    public void resetDemo() {
        dataInitializer.reset();
    }

    /**
     * 按最近 days 天构建登录趋势，缺失日期补 0，保证前端拿到固定长度的序列。
     */
    private List<TrendPoint> buildTrend(int days) {
        Map<String, Integer> byDay = new HashMap<>();
        SimpleDateFormat keyFormat = new SimpleDateFormat("yyyy-MM-dd");
        for (DailyLogin row : loginStatMapper.selectRecent(days)) {
            byDay.put(keyFormat.format(row.getStatDate()), row.getLoginCount());
        }

        SimpleDateFormat labelFormat = new SimpleDateFormat("M/d");
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -(days - 1));

        List<TrendPoint> points = new ArrayList<>(days);
        for (int i = 0; i < days; i++) {
            String key = keyFormat.format(calendar.getTime());
            Integer value = byDay.get(key);
            points.add(new TrendPoint(labelFormat.format(calendar.getTime()), value == null ? 0 : value));
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }
        return points;
    }
}
