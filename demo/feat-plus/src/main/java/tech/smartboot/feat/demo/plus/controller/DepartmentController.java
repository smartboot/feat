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
import tech.smartboot.feat.demo.plus.dto.DepartmentInputRequest;
import tech.smartboot.feat.demo.plus.dto.DepartmentNodeResponse;
import tech.smartboot.feat.demo.plus.security.AccessGuard;
import tech.smartboot.feat.demo.plus.security.Permission;
import tech.smartboot.feat.demo.plus.service.DepartmentService;
import tech.smartboot.feat.router.Context;

@Controller("api/departments")
public class DepartmentController extends ApiController {

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private AccessGuard accessGuard;

    /**
     * 返回部门树（含每个节点的直属成员数）。成员管理页的部门下拉也复用这一份数据。
     */
    @RequestMapping(value = "", method = RequestMethod.GET)
    public RestResult<List<DepartmentNodeResponse>> tree(Context context) {
        return execute(context, () -> {
            accessGuard.require(context, Permission.DEPT_READ);
            return departmentService.tree();
        });
    }

    @RequestMapping(value = "", method = RequestMethod.POST)
    public RestResult<DepartmentNodeResponse> create(Context context, DepartmentInputRequest request) {
        return execute(context, () -> {
            long operatorId = accessGuard.require(context, Permission.DEPT_WRITE).getUserId();
            return departmentService.create(request, operatorId);
        });
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
    public RestResult<DepartmentNodeResponse> update(Context context, @PathParam("id") String id,
                                                     DepartmentInputRequest request) {
        return execute(context, () -> {
            long operatorId = accessGuard.require(context, Permission.DEPT_WRITE).getUserId();
            return departmentService.update(parseId(id), request, operatorId);
        });
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public RestResult<Void> delete(Context context, @PathParam("id") String id) {
        return execute(context, () -> {
            long operatorId = accessGuard.require(context, Permission.DEPT_WRITE).getUserId();
            departmentService.delete(parseId(id), operatorId);
            return (Void) null;
        });
    }

    private static long parseId(String id) {
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException e) {
            throw DomainException.notFound("部门不存在或已被删除");
        }
    }
}
