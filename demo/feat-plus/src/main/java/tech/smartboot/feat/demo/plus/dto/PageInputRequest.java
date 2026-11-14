package tech.smartboot.feat.demo.plus.dto;

/**
 * 创建 / 更新页面配置的请求体。listJson / formJson 以 JSON 文本提交（第一阶段的配置管理页
 * 直接编辑 JSON），合法性由 PageService 反序列化 + 逐项校验兜住——存进去的一定能渲染。
 * <p>
 * 更新时 pageKey 被忽略：菜单 path 以 /p/{pageKey} 引用它，改 key 等于换身份。
 */
public class PageInputRequest {
    private String pageKey;
    private String title;
    private String description;
    private String datasourceKey;
    private String listJson;
    private String formJson;

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

    public String getListJson() {
        return listJson;
    }

    public void setListJson(String listJson) {
        this.listJson = listJson;
    }

    public String getFormJson() {
        return formJson;
    }

    public void setFormJson(String formJson) {
        this.formJson = formJson;
    }
}
