package tech.smartboot.feat.demo.plus.role.controller;

import java.util.List;

import tech.smartboot.feat.cloud.RestResult;
import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Controller;
import tech.smartboot.feat.cloud.annotation.PathParam;
import tech.smartboot.feat.cloud.annotation.RequestMapping;
import tech.smartboot.feat.cloud.annotation.RequestMethod;
import tech.smartboot.feat.demo.plus.auth.service.AccessGuard;
import tech.smartboot.feat.demo.plus.common.ApiController;
import tech.smartboot.feat.demo.plus.common.DomainException;
import tech.smartboot.feat.demo.plus.common.Permission;
import tech.smartboot.feat.demo.plus.role.dto.RoleInputRequest;
import tech.smartboot.feat.demo.plus.role.dto.RoleItemResponse;
import tech.smartboot.feat.demo.plus.role.dto.RoleStatusRequest;
import tech.smartboot.feat.demo.plus.role.service.RoleService;
import tech.smartboot.feat.router.Context;

@Controller("api/roles")
public class RoleController extends ApiController {

    @Autowired
    private RoleService roleService;

    @Autowired
    private AccessGuard accessGuard;

    @RequestMapping(value = "", method = RequestMethod.GET)
    public RestResult<List<RoleItemResponse>> list(Context context) {
        return execute(context, () -> {
            accessGuard.require(context, Permission.ROLE_READ);
            return roleService.list();
        });
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public RestResult<RoleItemResponse> detail(Context context, @PathParam("id") String id) {
        return execute(context, () -> {
            accessGuard.require(context, Permission.ROLE_READ);
            return roleService.get(parseId(id));
        });
    }

    @RequestMapping(value = "", method = RequestMethod.POST)
    public RestResult<RoleItemResponse> create(Context context, RoleInputRequest request) {
        return execute(context, () -> {
            long operatorId = accessGuard.require(context, Permission.ROLE_WRITE).getUserId();
            return roleService.create(request, operatorId);
        });
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
    public RestResult<RoleItemResponse> update(Context context, @PathParam("id") String id, RoleInputRequest request) {
        return execute(context, () -> {
            long operatorId = accessGuard.require(context, Permission.ROLE_WRITE).getUserId();
            return roleService.update(parseId(id), request, operatorId);
        });
    }

    @RequestMapping(value = "/{id}/status", method = RequestMethod.POST)
    public RestResult<RoleItemResponse> updateStatus(Context context, @PathParam("id") String id, RoleStatusRequest request) {
        return execute(context, () -> {
            long operatorId = accessGuard.require(context, Permission.ROLE_WRITE).getUserId();
            if (request == null || request.getStatus() == null) {
                throw new DomainException("缺少目标状态");
            }
            return roleService.setStatus(parseId(id), request.getStatus(), operatorId);
        });
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public RestResult<Void> delete(Context context, @PathParam("id") String id) {
        return execute(context, () -> {
            long operatorId = accessGuard.require(context, Permission.ROLE_WRITE).getUserId();
            roleService.delete(parseId(id), operatorId);
            return (Void) null;
        });
    }

    private static long parseId(String id) {
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException e) {
            throw DomainException.notFound("角色不存在或已被删除");
        }
    }
}
