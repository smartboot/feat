package tech.smartboot.feat.demo.plus.dashboard.dto;

import tech.smartboot.feat.demo.plus.common.DateFormats;
import tech.smartboot.feat.demo.plus.dashboard.model.NotificationRecord;

/**
 * 通知列表项，read 字段由 read_flag 转换而来。
 */
public class NotificationItemResponse {
    private final long id;
    private final String title;
    private final String description;
    private final String createdTime;
    private final boolean read;
    private final Long userId;

    public NotificationItemResponse(NotificationRecord record) {
        this.id = record.getId();
        this.title = record.getTitle();
        this.description = record.getDescription();
        this.createdTime = DateFormats.format(record.getCreatedTime());
        this.read = record.getReadFlag() != 0;
        this.userId = record.getUserId();
    }

    public long getId() {
        return id;
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

    public boolean isRead() {
        return read;
    }

    public Long getUserId() {
        return userId;
    }
}
