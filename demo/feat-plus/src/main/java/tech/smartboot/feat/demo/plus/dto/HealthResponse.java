package tech.smartboot.feat.demo.plus.dto;

/**
 * 健康探针的响应体。字段刻意保持恒定非空：
 * <ul>
 * <li>{@code status}：up / down，编排器（docker-compose healthcheck、k8s probe）只需要它；</li>
 * <li>{@code db}：ok / skipped / down。存活探针不查库，固定回 skipped——
 *     两个字段在两个探针上形状一致，消费方不用按探针类型分支；</li>
 * <li>{@code uptimeSeconds}：进程存活秒数，排查「刚启动就被打挂」时比日志时间戳直观。</li>
 * </ul>
 */
public class HealthResponse {
    private String status;
    private String db;
    private long uptimeSeconds;

    public HealthResponse() {
    }

    public HealthResponse(String status, String db, long uptimeSeconds) {
        this.status = status;
        this.db = db;
        this.uptimeSeconds = uptimeSeconds;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDb() {
        return db;
    }

    public void setDb(String db) {
        this.db = db;
    }

    public long getUptimeSeconds() {
        return uptimeSeconds;
    }

    public void setUptimeSeconds(long uptimeSeconds) {
        this.uptimeSeconds = uptimeSeconds;
    }
}
