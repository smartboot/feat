package tech.smartboot.feat.demo.plus.dashboard.dto;

import tech.smartboot.feat.demo.plus.common.DateFormats;
import tech.smartboot.feat.demo.plus.dashboard.model.ActivityRecord;

/**
 * 仪表盘上的操作动态项。
 */
public class ActivityItemResponse {
    private final long id;
    private final String kind;
    private final String title;
    private final String description;
    private final String createdAt;
    private final Long userId;

    public ActivityItemResponse(ActivityRecord record) {
        this.id = record.getId();
        this.kind = record.getKind();
        this.title = record.getTitle();
        this.description = record.getDescription();
        this.createdAt = DateFormats.format(record.getCreatedAt());
        this.userId = record.getUserId();
    }

    public long getId() {
        return id;
    }

    public String getKind() {
        return kind;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public Long getUserId() {
        return userId;
    }
}
