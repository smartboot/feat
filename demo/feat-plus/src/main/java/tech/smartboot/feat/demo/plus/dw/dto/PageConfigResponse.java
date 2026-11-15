package tech.smartboot.feat.demo.plus.dw.dto;

/**
 * {@code GET /api/dw/pages/{pageKey}} 的响应：一个动态页面的完整渲染配置。
 * <p>
 * list / form 是从 list_json / form_json 反序列化出来的结构（存原文、发结构），
 * 嵌套已到第四层，序列化走 fastjson2 反射路径——所有字段在 DTO 里都给了默认值，
 * 不依赖 null 语义，与「fastjson2 省略 null 字段」的既有约定对齐。
 * <p>
 * {@code canWrite} 按「当前请求人是否拥有数据源的写权限点」逐请求计算，绝不进缓存：
 * 配置是共享的，写权限是个人的。
 */
public class PageConfigResponse {
    private String pageKey;
    private String title;
    private String description;
    private String datasourceKey;
    private DwListConfig list = new DwListConfig();
    private DwFormConfig form = new DwFormConfig();
    private boolean canWrite;

    public String getPageKey() {
        return pageKey;
    }

    public void setPageKey(String pageKey) {
        this.pageKey = pageKey;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDatasourceKey() {
        return datasourceKey;
    }

    public void setDatasourceKey(String datasourceKey) {
        this.datasourceKey = datasourceKey;
    }

    public DwListConfig getList() {
        return list;
    }

    public void setList(DwListConfig list) {
        this.list = list;
    }

    public DwFormConfig getForm() {
        return form;
    }

    public void setForm(DwFormConfig form) {
        this.form = form;
    }

    public boolean isCanWrite() {
        return canWrite;
    }

    public void setCanWrite(boolean canWrite) {
        this.canWrite = canWrite;
    }
}
