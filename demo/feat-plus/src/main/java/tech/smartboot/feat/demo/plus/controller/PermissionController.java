package tech.smartboot.feat.demo.plus.controller;

import java.util.List;

import tech.smartboot.feat.cloud.RestResult;
import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Controller;
import tech.smartboot.feat.cloud.annotation.RequestMapping;
import tech.smartboot.feat.cloud.annotation.RequestMethod;
import tech.smartboot.feat.demo.plus.common.ApiController;
import tech.smartboot.feat.demo.plus.dto.PermissionItemResponse;
import tech.smartboot.feat.demo.plus.security.AccessGuard;
import tech.smartboot.feat.demo.plus.security.Permission;
import tech.smartboot.feat.demo.plus.service.PermissionService;
import tech.smartboot.feat.router.Context;

/**
 * 权限点字典。
 * <p>
 * 不要挂在 {@code api/roles} 下：那里已有 {@code GET /{id}}，再加一条 {@code /permissions}
 * 就变成「字面量路径与占位符路径谁先匹配」的问题，行为取决于路由注册顺序，而顺序是编译期生成的。
 * 独立一个前缀，永远不会和占位符打架。
 */
@Controller("api/permissions")
public class PermissionController extends ApiController {

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private AccessGuard accessGuard;

    /**
     * 需要 role:read 而不是「登录即可」：字典的唯一用途是配置角色，
     * 没有这个权限点的人打开角色页也会被其它接口拦下，早点拒绝比给出半份数据清楚。
     */
    @RequestMapping(value = "", method = RequestMethod.GET)
    public RestResult<List<PermissionItemResponse>> list(Context context) {
        return execute(context, () -> {
            accessGuard.require(context, Permission.ROLE_READ);
            return permissionService.catalog();
        });
    }
}
