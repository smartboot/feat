package tech.smartboot.feat.demo.plus.controller;

import java.util.HashMap;
import java.util.Map;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;

import tech.smartboot.feat.cloud.RestResult;
import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Controller;
import tech.smartboot.feat.cloud.annotation.PathParam;
import tech.smartboot.feat.cloud.annotation.Param;
import tech.smartboot.feat.cloud.annotation.RequestMapping;
import tech.smartboot.feat.cloud.annotation.RequestMethod;
import tech.smartboot.feat.demo.plus.common.ApiController;
import tech.smartboot.feat.demo.plus.common.DomainException;
import tech.smartboot.feat.demo.plus.common.PageResult;
import tech.smartboot.feat.demo.plus.dto.DwSaveRequest;
import tech.smartboot.feat.demo.plus.dto.PageConfigResponse;
import tech.smartboot.feat.demo.plus.dto.PageDataQuery;
import tech.smartboot.feat.demo.plus.security.AccessContext;
import tech.smartboot.feat.demo.plus.security.AccessGuard;
import tech.smartboot.feat.demo.plus.service.PageDataSource;
import tech.smartboot.feat.demo.plus.service.PageDataSourceRegistry;
import tech.smartboot.feat.demo.plus.service.PageService;
import tech.smartboot.feat.router.Context;

/**
 * 动态页面的渲染与数据通道。
 * <p>
 * 一条路由族 /api/dw/pages/{pageKey}/... 同时服务三种请求：拉配置（渲染）、拉数据（列表）、
 * 写数据（保存 / 删除）。权限统一在 Controller 按<b>数据源声明的权限点</b> require——
 * 权限点编译期写死在 DataSource 实现里，配置表只引用数据源，不引用权限点。
 * <p>
 * 读取配置的门槛 = 数据源的读权限：能看页面的人才能拿配置；写操作的门槛 = 写权限点，
 * 与页面配置里的 canWrite（按同一权限点算出）保持一致，不会出现「按钮亮着、保存 403」之外的第二套真相。
 */
@Controller("api/dw")
public class DwController extends ApiController {

    @Autowired
    private PageService pageService;

    // 字段名必须等于 bean 名（@Autowired 按名注入，AOT 硬约束）。
    @Autowired
    private PageDataSourceRegistry pageDataSourceRegistry;

    @Autowired
    private AccessGuard accessGuard;

    /** 页面渲染配置。canWrite 按当前请求人逐次计算，不进缓存。 */
    @RequestMapping(value = "/pages/{pageKey}", method = RequestMethod.GET)
    public RestResult<PageConfigResponse> config(Context context, @PathParam("pageKey") String pageKey) {
        return execute(context, () -> {
            PageDataSource dataSource = dataSourceOf(pageKey);
            AccessContext access = accessGuard.require(context, dataSource.readPermission());
            return pageService.config(pageKey, access.can(dataSource.writePermission()));
        });
    }

    /** 列表数据：分页 + 筛选（filter 是前端 JSON.stringify 的键值对）。 */
    @RequestMapping(value = "/pages/{pageKey}/data", method = RequestMethod.GET)
    public RestResult<PageResult<Map<String, Object>>> list(Context context,
            @PathParam("pageKey") String pageKey, @Param("page") Integer page,
            @Param("pageSize") Integer pageSize, @Param("filter") String filter) {
        return execute(context, () -> {
            PageDataSource dataSource = dataSourceOf(pageKey);
            AccessContext access = accessGuard.require(context, dataSource.readPermission());
            return dataSource.list(queryOf(page, pageSize, filter), access);
        });
    }

    /** 单行数据，表单编辑回填用。 */
    @RequestMapping(value = "/pages/{pageKey}/data/{id}", method = RequestMethod.GET)
    public RestResult<Map<String, Object>> detail(Context context, @PathParam("pageKey") String pageKey,
                                                  @PathParam("id") String id) {
        return execute(context, () -> {
            PageDataSource dataSource = dataSourceOf(pageKey);
            AccessContext access = accessGuard.require(context, dataSource.readPermission());
            return dataSource.get(parseId(id), access);
        });
    }

    /** 保存：请求体里 id 为 null 表示新建，否则按 id 更新。 */
    @RequestMapping(value = "/pages/{pageKey}/data", method = RequestMethod.POST)
    public RestResult<Map<String, Object>> save(Context context, @PathParam("pageKey") String pageKey,
                                                 DwSaveRequest request) {
        return execute(context, () -> {
            PageDataSource dataSource = dataSourceOf(pageKey);
            AccessContext access = accessGuard.require(context, dataSource.writePermission());
            return dataSource.save(request, access);
        });
    }

    @RequestMapping(value = "/pages/{pageKey}/data/{id}", method = RequestMethod.DELETE)
    public RestResult<Void> remove(Context context, @PathParam("pageKey") String pageKey,
                                   @PathParam("id") String id) {
        return execute(context, () -> {
            PageDataSource dataSource = dataSourceOf(pageKey);
            AccessContext access = accessGuard.require(context, dataSource.writePermission());
            dataSource.delete(parseId(id), access);
            return (Void) null;
        });
    }

    private PageDataSource dataSourceOf(String pageKey) {
        // 先确认配置行存在（404），再取数据源——配置行里的 datasourceKey 保存时已过白名单校验。
        return pageDataSourceRegistry.require(pageService.requireDatasourceKey(pageKey));
    }

    /**
     * 查询参数组装：filter 解析失败按空过滤器处理。它的值只喂给类型化的 DataSource 解释，
     * 不参与任何 SQL 拼接，所以宽松处理没有注入面。
     */
    private static PageDataQuery queryOf(Integer page, Integer pageSize, String filter) {
        PageDataQuery query = new PageDataQuery();
        if (page != null) {
            query.setPage(page);
        }
        if (pageSize != null) {
            query.setPageSize(pageSize);
        }
        Map<String, String> filters = new HashMap<>();
        if (filter != null && !filter.trim().isEmpty()) {
            try {
                Map<String, Object> raw = JSON.parseObject(filter);
                if (raw != null) {
                    for (Map.Entry<String, Object> entry : raw.entrySet()) {
                        if (entry.getValue() != null) {
                            filters.put(entry.getKey(), String.valueOf(entry.getValue()));
                        }
                    }
                }
            } catch (JSONException e) {
                // 容错：坏 filter 不值得 500，按没有筛选条件继续。
            }
        }
        query.setFilters(filters);
        return query;
    }

    private static long parseId(String id) {
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException e) {
            throw DomainException.notFound("数据不存在或已被删除");
        }
    }
}
