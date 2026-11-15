package tech.smartboot.feat.demo.plus.dw.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 列表页渲染描述，即 sys_page.list_json 的结构。
 * <p>
 * actions 是内置标准动作的子集（create / edit / delete / refresh）——
 * 老平台把按钮 JS 整段存库的能力刻意不再支持，自定义动作等后续用别的方式替换。
 * <p>
 * dataApi 是数据通道覆盖（见 {@link DwDataApi}）：只覆盖查询与删除，渲染描述不变。
 */
public class DwListConfig {
    private List<DwColumn> columns = new ArrayList<>();
    private List<DwFilter> filters = new ArrayList<>();
    private int pageSize = 20;
    private List<String> actions = new ArrayList<>();
    private String rowKey = "id";
    private DwDataApi dataApi = new DwDataApi();

    public List<DwColumn> getColumns() {
        return columns;
    }

    public void setColumns(List<DwColumn> columns) {
        this.columns = columns == null ? new ArrayList<>() : columns;
    }

    public List<DwFilter> getFilters() {
        return filters;
    }

    public void setFilters(List<DwFilter> filters) {
        this.filters = filters == null ? new ArrayList<>() : filters;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public List<String> getActions() {
        return actions;
    }

    public void setActions(List<String> actions) {
        this.actions = actions == null ? new ArrayList<>() : actions;
    }

    public String getRowKey() {
        return rowKey;
    }

    public void setRowKey(String rowKey) {
        this.rowKey = rowKey;
    }

    public DwDataApi getDataApi() {
        return dataApi;
    }

    public void setDataApi(DwDataApi dataApi) {
        this.dataApi = dataApi == null ? new DwDataApi() : dataApi;
    }
}
