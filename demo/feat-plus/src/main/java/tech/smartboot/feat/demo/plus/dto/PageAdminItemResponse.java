package tech.smartboot.feat.demo.plus.dto;

import tech.smartboot.feat.demo.plus.common.DateFormats;
import tech.smartboot.feat.demo.plus.model.PageRecord;

/**
 * 配置管理页的一行，来自 {@code GET /api/pages}。
 * <p>
 * listJson / formJson 原文下发：管理页的编辑框要展示并回传原文，
 * 与渲染面拿到的结构化 PageConfigResponse 是两种消费形态，不共用 DTO。
 */
public class PageAdminItemResponse {
    private long id;
    private String pageKey;
    private String title;
    private String description;
    private String datasourceKey;
    private String listJson;
    private String formJson;
    private String editTime;

    public static PageAdminItemResponse from(PageRecord row) {
        PageAdminItemResponse item = new PageAdminItemResponse();
        item.id = row.getId();
        item.pageKey = row.getPageKey();
        item.title = row.getTitle();
        item.description = row.getDescription();
        item.datasourceKey = row.getDatasourceKey();
        item.listJson = row.getListJson();
        item.formJson = row.getFormJson();
        item.editTime = DateFormats.format(row.getEditTime());
        return item;
    }

    public long getId() {
        return id;
    }

    public String getPageKey() {
        return pageKey;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getDatasourceKey() {
        return datasourceKey;
    }

    public String getListJson() {
        return listJson;
    }

    public String getFormJson() {
        return formJson;
    }

    public String getEditTime() {
        return editTime;
    }
}
