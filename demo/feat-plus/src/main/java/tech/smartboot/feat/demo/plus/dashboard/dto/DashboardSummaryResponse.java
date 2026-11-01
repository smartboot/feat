package tech.smartboot.feat.demo.plus.dashboard.dto;

import java.util.List;

/**
 * 仪表盘汇总数据，字段与前端 DashboardSummary 对齐。
 */
public class DashboardSummaryResponse {
    private final long totalUsers;
    private final long enabledUsers;
    private final long disabledUsers;
    private final long unreadNotifications;
    private final List<TrendPoint> trend7;
    private final List<TrendPoint> trend30;
    private final List<ActivityItemResponse> activities;

    public DashboardSummaryResponse(long totalUsers, long enabledUsers, long disabledUsers,
                                    long unreadNotifications, List<TrendPoint> trend7,
                                    List<TrendPoint> trend30, List<ActivityItemResponse> activities) {
        this.totalUsers = totalUsers;
        this.enabledUsers = enabledUsers;
        this.disabledUsers = disabledUsers;
        this.unreadNotifications = unreadNotifications;
        this.trend7 = trend7;
        this.trend30 = trend30;
        this.activities = activities;
    }

    public long getTotalUsers() {
        return totalUsers;
    }

    public long getEnabledUsers() {
        return enabledUsers;
    }

    public long getDisabledUsers() {
        return disabledUsers;
    }

    public long getUnreadNotifications() {
        return unreadNotifications;
    }

    public List<TrendPoint> getTrend7() {
        return trend7;
    }

    public List<TrendPoint> getTrend30() {
        return trend30;
    }

    public List<ActivityItemResponse> getActivities() {
        return activities;
    }
}
