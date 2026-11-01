package tech.smartboot.feat.demo.plus.user.controller;

import java.util.List;

import tech.smartboot.feat.cloud.RestResult;
import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Controller;
import tech.smartboot.feat.cloud.annotation.Param;
import tech.smartboot.feat.cloud.annotation.PathParam;
import tech.smartboot.feat.cloud.annotation.RequestMapping;
import tech.smartboot.feat.cloud.annotation.RequestMethod;
import tech.smartboot.feat.demo.plus.auth.service.AuthGuard;
import tech.smartboot.feat.demo.plus.auth.token.TokenStore;
import tech.smartboot.feat.demo.plus.common.ApiController;
import tech.smartboot.feat.demo.plus.common.DomainException;
import tech.smartboot.feat.demo.plus.common.page.PageResult;
import tech.smartboot.feat.demo.plus.user.dto.TemporaryPasswordResponse;
import tech.smartboot.feat.demo.plus.user.dto.UserInputRequest;
import tech.smartboot.feat.demo.plus.user.dto.UserItemResponse;
import tech.smartboot.feat.demo.plus.user.dto.UserStatusRequest;
import tech.smartboot.feat.demo.plus.user.service.UserService;
import tech.smartboot.feat.router.Context;

@Controller("api/users")
public class UserController extends ApiController {

    @Autowired
    private UserService userService;

    @Autowired
    private TokenStore tokenStore;

    @RequestMapping(value = "", method = RequestMethod.GET)
    public RestResult<PageResult<UserItemResponse>> list(
            Context context,
            @Param("page") Integer page,
            @Param("pageSize") Integer pageSize,
            @Param("keyword") String keyword,
            @Param("status") String status,
            @Param("roleCode") String roleCode,
            @Param("department") String department) {
        return execute(context, () -> {
            AuthGuard.require(context, tokenStore);
            return userService.list(keyword, parseStatus(status), roleCode, department,
                    page == null ? 1 : page, pageSize == null ? 10 : pageSize);
        });
    }

    @RequestMapping(value = "/all", method = RequestMethod.GET)
    public RestResult<List<UserItemResponse>> listAll(Context context) {
        return execute(context, () -> {
            AuthGuard.require(context, tokenStore);
            return userService.listAll();
        });
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public RestResult<UserItemResponse> detail(Context context, @PathParam("id") String id) {
        return execute(context, () -> {
            AuthGuard.require(context, tokenStore);
            return userService.get(parseId(id));
        });
    }

    @RequestMapping(value = "", method = RequestMethod.POST)
    public RestResult<UserItemResponse> create(Context context, UserInputRequest request) {
        return execute(context, () -> {
            long operatorId = AuthGuard.require(context, tokenStore);
            return userService.create(request, operatorId);
        });
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
    public RestResult<UserItemResponse> update(Context context, @PathParam("id") String id, UserInputRequest request) {
        return execute(context, () -> {
            long operatorId = AuthGuard.require(context, tokenStore);
            return userService.update(parseId(id), request, operatorId);
        });
    }

    @RequestMapping(value = "/{id}/status", method = RequestMethod.POST)
    public RestResult<UserItemResponse> updateStatus(Context context, @PathParam("id") String id, UserStatusRequest request) {
        return execute(context, () -> {
            long operatorId = AuthGuard.require(context, tokenStore);
            if (request == null || request.getStatus() == null) {
                throw new DomainException("缺少目标状态");
            }
            return userService.setStatus(parseId(id), request.getStatus(), operatorId);
        });
    }

    @RequestMapping(value = "/{id}/reset-password", method = RequestMethod.POST)
    public RestResult<TemporaryPasswordResponse> resetPassword(Context context, @PathParam("id") String id) {
        return execute(context, () -> {
            long operatorId = AuthGuard.require(context, tokenStore);
            return new TemporaryPasswordResponse(userService.resetPassword(parseId(id), operatorId));
        });
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public RestResult<Void> delete(Context context, @PathParam("id") String id) {
        return execute(context, () -> {
            long operatorId = AuthGuard.require(context, tokenStore);
            userService.delete(parseId(id), operatorId);
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
}
