package tech.smartboot.feat.demo.plus.controller;

import java.util.List;

import tech.smartboot.feat.cloud.RestResult;
import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Controller;
import tech.smartboot.feat.cloud.annotation.Param;
import tech.smartboot.feat.cloud.annotation.PathParam;
import tech.smartboot.feat.cloud.annotation.RequestMapping;
import tech.smartboot.feat.cloud.annotation.RequestMethod;
import tech.smartboot.feat.demo.plus.common.ApiController;
import tech.smartboot.feat.demo.plus.common.DomainException;
import tech.smartboot.feat.demo.plus.common.PageResult;
import tech.smartboot.feat.demo.plus.dto.TemporaryPasswordResponse;
import tech.smartboot.feat.demo.plus.dto.UserInputRequest;
import tech.smartboot.feat.demo.plus.dto.UserItemResponse;
import tech.smartboot.feat.demo.plus.dto.UserStatusRequest;
import tech.smartboot.feat.demo.plus.security.AccessContext;
import tech.smartboot.feat.demo.plus.security.AccessGuard;
import tech.smartboot.feat.demo.plus.security.Permission;
import tech.smartboot.feat.demo.plus.service.UserService;
import tech.smartboot.feat.router.Context;

@Controller("api/users")
public class UserController extends ApiController {

    @Autowired
    private UserService userService;

    @Autowired
    private AccessGuard accessGuard;

    @RequestMapping(value = "", method = RequestMethod.GET)
    public RestResult<PageResult<UserItemResponse>> list(
            Context context,
            @Param("page") Integer page,
            @Param("pageSize") Integer pageSize,
            @Param("keyword") String keyword,
            @Param("status") String status,
            @Param("roleCode") String roleCode,
            @Param("departmentId") String departmentId) {
        return execute(context, () -> {
            AccessContext access = accessGuard.require(context, Permission.USER_READ);
            return userService.list(keyword, parseStatus(status), roleCode, parseDepartmentId(departmentId),
                    page == null ? 1 : page, pageSize == null ? 10 : pageSize, access);
        });
    }

    @RequestMapping(value = "/all", method = RequestMethod.GET)
    public RestResult<List<UserItemResponse>> listAll(Context context) {
        return execute(context, () -> {
            AccessContext access = accessGuard.require(context, Permission.USER_READ);
            return userService.listAll(access);
        });
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public RestResult<UserItemResponse> detail(Context context, @PathParam("id") String id) {
        return execute(context, () -> {
            AccessContext access = accessGuard.require(context, Permission.USER_READ);
            return userService.get(parseId(id), access);
        });
    }

    @RequestMapping(value = "", method = RequestMethod.POST)
    public RestResult<UserItemResponse> create(Context context, UserInputRequest request) {
        return execute(context, () -> {
            AccessContext access = accessGuard.require(context, Permission.USER_WRITE);
            return userService.create(request, access);
        });
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
    public RestResult<UserItemResponse> update(Context context, @PathParam("id") String id, UserInputRequest request) {
        return execute(context, () -> {
            AccessContext access = accessGuard.require(context, Permission.USER_WRITE);
            return userService.update(parseId(id), request, access);
        });
    }

    @RequestMapping(value = "/{id}/status", method = RequestMethod.POST)
    public RestResult<UserItemResponse> updateStatus(Context context, @PathParam("id") String id, UserStatusRequest request) {
        return execute(context, () -> {
            AccessContext access = accessGuard.require(context, Permission.USER_WRITE);
            if (request == null || request.getStatus() == null) {
                throw new DomainException("缺少目标状态");
            }
            return userService.setStatus(parseId(id), request.getStatus(), access);
        });
    }

    @RequestMapping(value = "/{id}/reset-password", method = RequestMethod.POST)
    public RestResult<TemporaryPasswordResponse> resetPassword(Context context, @PathParam("id") String id) {
        return execute(context, () -> {
            AccessContext access = accessGuard.require(context, Permission.USER_WRITE);
            return new TemporaryPasswordResponse(userService.resetPassword(parseId(id), access));
        });
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public RestResult<Void> delete(Context context, @PathParam("id") String id) {
        return execute(context, () -> {
            AccessContext access = accessGuard.require(context, Permission.USER_WRITE);
            userService.delete(parseId(id), access);
            return (Void) null;
        });
    }

    private static long parseId(String id) {
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException e) {
            throw DomainException.notFound("用户不存在或已被删除");
        }
    }

    private static Integer parseStatus(String status) {
        if (status == null || status.trim().isEmpty() || "all".equalsIgnoreCase(status.trim())) {
            return null;
        }
        try {
            return Integer.valueOf(status.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 空串与 all 都表示「全部部门」，其余必须是数字 id。
     */
    private static Long parseDepartmentId(String departmentId) {
        if (departmentId == null || departmentId.trim().isEmpty() || "all".equalsIgnoreCase(departmentId.trim())) {
            return null;
        }
        try {
            return Long.valueOf(departmentId.trim());
        } catch (NumberFormatException e) {
            throw new DomainException("部门参数不合法");
        }
    }
}
