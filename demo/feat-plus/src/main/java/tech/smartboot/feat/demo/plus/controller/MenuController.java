package tech.smartboot.feat.demo.plus.controller;

import java.util.List;

import tech.smartboot.feat.cloud.RestResult;
import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Controller;
import tech.smartboot.feat.cloud.annotation.PathParam;
import tech.smartboot.feat.cloud.annotation.RequestMapping;
import tech.smartboot.feat.cloud.annotation.RequestMethod;
import tech.smartboot.feat.demo.plus.common.ApiController;
import tech.smartboot.feat.demo.plus.common.DomainException;
import tech.smartboot.feat.demo.plus.dto.MenuAdminItemResponse;
import tech.smartboot.feat.demo.plus.dto.MenuInputRequest;
import tech.smartboot.feat.demo.plus.security.AccessGuard;
import tech.smartboot.feat.demo.plus.security.Permission;
import tech.smartboot.feat.demo.plus.service.MenuService;
import tech.smartboot.feat.router.Context;

/**
 * 菜单配置。
 * <p>
 * 与「下发菜单」是两条独立的路径：下发走 {@code /api/auth/login} 与 {@code /api/auth/me}（跟着当前登录人），
 * 这里是配置面，只有 {@code menu:read} / {@code menu:write} 的人能进。
 * 分开的理由是缓存与权限都不同——下发面每请求一次、只读；配置面低频、要写。
 */
@Controller("api/menus")
public class MenuController extends ApiController {

    @Autowired
    private MenuService menuService;

    @Autowired
    private AccessGuard accessGuard;

    /**
     * 全量菜单（含不进侧栏的项）。角色页、用户页不需要它，只有菜单管理页用。
     */
    @RequestMapping(value = "", method = RequestMethod.GET)
    public RestResult<List<MenuAdminItemResponse>> list(Context context) {
        return execute(context, () -> {
            accessGuard.require(context, Permission.MENU_READ);
            return menuService.list();
        });
    }

    /**
     * 可选页面路径。前端把它渲染成下拉框，而不是让人手填——
     * 手填错的后果是「菜单点进去被弹回首页」，这种错误在数据上完全看不出来。
     */
    @RequestMapping(value = "/routes", method = RequestMethod.GET)
    public RestResult<List<String>> routes(Context context) {
        return execute(context, () -> {
            accessGuard.require(context, Permission.MENU_WRITE);
            return menuService.routeOptions();
        });
    }

    @RequestMapping(value = "", method = RequestMethod.POST)
    public RestResult<MenuAdminItemResponse> create(Context context, MenuInputRequest request) {
        return execute(context, () -> {
            long operatorId = accessGuard.require(context, Permission.MENU_WRITE).getUserId();
            return menuService.create(request, operatorId);
        });
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
    public RestResult<MenuAdminItemResponse> update(Context context, @PathParam("id") String id,
                                                    MenuInputRequest request) {
        return execute(context, () -> {
            long operatorId = accessGuard.require(context, Permission.MENU_WRITE).getUserId();
            return menuService.update(parseId(id), request, operatorId);
        });
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public RestResult<Void> delete(Context context, @PathParam("id") String id) {
        return execute(context, () -> {
            long operatorId = accessGuard.require(context, Permission.MENU_WRITE).getUserId();
            menuService.delete(parseId(id), operatorId);
            return (Void) null;
        });
    }

    private static long parseId(String id) {
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException e) {
            throw DomainException.notFound("菜单不存在或已被删除");
        }
    }
}
