package tech.smartboot.feat.demo.plus.dw.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 列表页渲染描述，即 sys_page.list_json 的结构。
 * <p>
 * actions 是内置标准动作的子集（create / edit / delete / refresh）——
 * 老平台把按钮 JS 整段存库的能力刻意不再支持，需要「重置密码」这类单行操作时，
 * 用 {@link #rowActions} 声明一个按钮 + 一个站内接口，见 {@link DwRowAction}。
 * <p>
 * dataApi 是数据通道覆盖（见 {@link DwDataApi}）：只覆盖查询与删除，渲染描述不变。
 * <p>
 * {@link #fieldMaps} 放在这里存，但它服务的是整张页面——见该字段的说明。
 */
public class DwListConfig {
    private List<DwColumn> columns = new ArrayList<>();
    private List<DwFilter> filters = new ArrayList<>();
    private int pageSize = 20;
    private List<String> actions = new ArrayList<>();
    /** 自定义行操作：与编辑 / 删除同列在行操作菜单里，排在它们之后。 */
    private List<DwRowAction> rowActions = new ArrayList<>();
    /**
     * 本页共享的「字段值域」：字段 key → 值 → 文案映射。
     * <p>
     * 一个字段常常同时出现在列表列、表单下拉、筛选下拉里。映射配在各视图上，就会
     * 「列表显示启用、筛选下拉显示 1」，而且改一处忘一处。值域是字段的事实、不是视图的表达，
     * 所以放在页面级只有一份：视图只声明怎么画，值域从这里取。
     * <p>
     * 视图自己配了 options 的，用视图的（覆盖）；没配的继承这里。
     * 存储落在 list_json 是工程折中（不为一个 map 新建一列），语义上面向整页。
     * <p>
     * 键序由 {@link #setFieldMaps} 统一成「key 字典序」，这里声明成 {@code Map} 是给 AOT 看的：
     * Feat 的注解处理器只认 Map / List 这类明确类型，字段写成 {@code LinkedHashMap} 会退回
     * 运行时反射序列化；而 fastjson2 反序列化又按<b>声明类型</b>造实例，声明成 Map 就给 HashMap，
     * 键序由 hash 与容量决定——实测同一批 key 从 6 个增到 13 个（越过 resize 阈值）时，
     * 原有键的相对次序会整体改变。两难各让一步：类型留 Map 给 AOT，顺序在 setter 里定。
     * <p>
     * 顺序在这里不代表业务含义（消费方按 key 直查，界面按数据源字段顺序展示），
     * 它只需要「确定」——否则只改一处配置，list_json 文本却整体变动，版本对比与审计全是噪声。
     */
    private Map<String, List<DwOption>> fieldMaps = new LinkedHashMap<>();
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

    public List<DwRowAction> getRowActions() {
        return rowActions;
    }

    public void setRowActions(List<DwRowAction> rowActions) {
        this.rowActions = rowActions == null ? new ArrayList<>() : rowActions;
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

    public Map<String, List<DwOption>> getFieldMaps() {
        return fieldMaps;
    }

    /**
     * 按 key 字典序重建，把 fastjson2 造出来的 HashMap 顺序「钉死」。
     * <p>
     * 为什么不是保留 JSON 里的书写顺序：那需要字段声明成 {@link LinkedHashMap}，AOT 编译器
     * 会因此退回运行时反射（见字段注释）。而书写顺序本来就没有业务含义，字典序满足同样的
     * 目的——它只要求「同一份逻辑配置 → 同一段文本」。字典序还有一个 HashMap 给不了的性质：
     * 新增一个 key 只插入它自己，其余 key 的相对次序不动。
     */
    public void setFieldMaps(Map<String, List<DwOption>> fieldMaps) {
        LinkedHashMap<String, List<DwOption>> ordered = new LinkedHashMap<>();
        if (fieldMaps != null) {
            List<String> keys = new ArrayList<>(fieldMaps.keySet());
            Collections.sort(keys);
            for (String key : keys) {
                ordered.put(key, fieldMaps.get(key));
            }
        }
        this.fieldMaps = ordered;
    }
}
