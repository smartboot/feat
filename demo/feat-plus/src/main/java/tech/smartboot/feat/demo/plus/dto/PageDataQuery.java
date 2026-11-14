package tech.smartboot.feat.demo.plus.dto;

import java.util.Collections;
import java.util.Map;

/**
 * 动态列表的数据查询参数：分页 + 筛选键值对。
 * <p>
 * filters 的 key 来自页面配置（PageService 已校验过命中数据源字段），值的解释权在
 * DataSource 实现里——它是类型化解释，不参与任何 SQL 拼接，所以这里不需要防注入。
 */
public class PageDataQuery {
    private int page = 1;
    private int pageSize = 20;
    private Map<String, String> filters = Collections.emptyMap();

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public Map<String, String> getFilters() {
        return filters;
    }

    public void setFilters(Map<String, String> filters) {
        this.filters = filters == null ? Collections.<String, String>emptyMap() : filters;
    }
}
