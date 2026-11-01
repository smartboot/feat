package tech.smartboot.feat.demo.plus.dashboard.dto;

import tech.smartboot.feat.demo.plus.common.DateFormats;
import tech.smartboot.feat.demo.plus.dashboard.model.ActivityRecord;

/**
 * 仪表盘上的操作动态项。
 * <p>
 * userId 是动态发生在谁身上，creator 是执行操作的人。
 */
public class ActivityItemResponse {
    private final long id;
    private final String kind;
    private final String title;
    private final String description;
    private final String createdTime;
    private final Long userId;
    private final Long creator;

    public ActivityItemResponse(ActivityRecord record) {
        this.id = record.getId();
        this.kind = record.getKind();
        this.title = record.getTitle();
        this.description = record.getDescription();
        this.createdTime = DateFormats.format(record.getCreatedTime());
        this.userId = record.getUserId();
        this.creator = record.getCreator();
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

    public String getCreatedTime() {
        return createdTime;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getCreator() {
        return creator;
    }
}
