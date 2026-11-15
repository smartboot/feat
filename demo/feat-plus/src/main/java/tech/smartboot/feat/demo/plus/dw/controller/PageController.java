package tech.smartboot.feat.demo.plus.dw.controller;

import java.util.List;

import tech.smartboot.feat.cloud.RestResult;
import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Controller;
import tech.smartboot.feat.cloud.annotation.PathParam;
import tech.smartboot.feat.cloud.annotation.RequestMapping;
import tech.smartboot.feat.cloud.annotation.RequestMethod;
import tech.smartboot.feat.demo.plus.common.ApiController;
import tech.smartboot.feat.demo.plus.common.DomainException;
import tech.smartboot.feat.demo.plus.dw.dto.DatasourceOptionResponse;
import tech.smartboot.feat.demo.plus.dw.dto.PageAdminItemResponse;
import tech.smartboot.feat.demo.plus.dw.dto.PageInputRequest;
import tech.smartboot.feat.demo.plus.security.AccessGuard;
import tech.smartboot.feat.demo.plus.security.Permission;
import tech.smartboot.feat.demo.plus.dw.service.PageService;
import tech.smartboot.feat.router.Context;

/**
 * 动态页面配置面（管理页专用）。与渲染面（/api/dw）分开：
 * 渲染面按数据源自己的权限点放行、每个打开页面的人都要走；
 * 这里是配置面，page:read / page:write 独立成点——改的是别人页面的长相，风险等级不同。
 * <p>
 * {@code /datasources} 是字面量子路径，与 {@code /{id}} 不在同一 HTTP 方法下混用，
 * 与 PermissionController 独立成前缀是同一个考虑。
 */
@Controller("api/pages")
public class PageController extends ApiController {

    @Autowired
    private PageService pageService;

    @Autowired
    private AccessGuard accessGuard;

    @RequestMapping(value = "", method = RequestMethod.GET)
    public RestResult<List<PageAdminItemResponse>> list(Context context) {
        return execute(context, () -> {
            accessGuard.require(context, Permission.PAGE_READ);
            return pageService.list();
        });
    }

    /** 可选数据源 + 字段声明，配置表单的下拉与字段提示用。 */
    @RequestMapping(value = "/datasources", method = RequestMethod.GET)
    public RestResult<List<DatasourceOptionResponse>> datasources(Context context) {
        return execute(context, () -> {
            accessGuard.require(context, Permission.PAGE_READ);
            return pageService.datasourceOptions();
        });
    }

    @RequestMapping(value = "", method = RequestMethod.POST)
    public RestResult<PageAdminItemResponse> create(Context context, PageInputRequest request) {
        return execute(context, () -> {
            long operatorId = accessGuard.require(context, Permission.PAGE_WRITE).getUserId();
            return pageService.create(request, operatorId);
        });
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
    public RestResult<PageAdminItemResponse> update(Context context, @PathParam("id") String id,
                                                     PageInputRequest request) {
        return execute(context, () -> {
            long operatorId = accessGuard.require(context, Permission.PAGE_WRITE).getUserId();
            return pageService.update(parseId(id), request, operatorId);
        });
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public RestResult<Void> delete(Context context, @PathParam("id") String id) {
        return execute(context, () -> {
            long operatorId = accessGuard.require(context, Permission.PAGE_WRITE).getUserId();
            pageService.delete(parseId(id), operatorId);
            return (Void) null;
        });
    }

    private static long parseId(String id) {
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException e) {
            throw DomainException.notFound("页面配置不存在或已被删除");
        }
    }
}
