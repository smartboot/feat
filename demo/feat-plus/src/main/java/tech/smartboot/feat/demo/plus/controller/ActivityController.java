package tech.smartboot.feat.demo.plus.controller;

import java.util.List;

import tech.smartboot.feat.cloud.RestResult;
import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Controller;
import tech.smartboot.feat.cloud.annotation.Param;
import tech.smartboot.feat.cloud.annotation.RequestMapping;
import tech.smartboot.feat.cloud.annotation.RequestMethod;
import tech.smartboot.feat.demo.plus.common.ApiController;
import tech.smartboot.feat.demo.plus.common.DomainException;
import tech.smartboot.feat.demo.plus.common.PageResult;
import tech.smartboot.feat.demo.plus.dto.ActivityItemResponse;
import tech.smartboot.feat.demo.plus.dto.OperatorOptionResponse;
import tech.smartboot.feat.demo.plus.security.AccessContext;
import tech.smartboot.feat.demo.plus.security.AccessGuard;
import tech.smartboot.feat.demo.plus.security.Permission;
import tech.smartboot.feat.demo.plus.service.ActivityService;
import tech.smartboot.feat.router.Context;

/**
 * 审计日志。只提供查询，没有任何写接口——日志只能由系统在业务操作时写入。
 * <p>
 * 权限点用独立的 audit:read：能看到「谁改了谁」比能看用户列表更敏感，不该复用 user:read。
 */
@Controller("api/activities")
public class ActivityController extends ApiController {

    @Autowired
    private ActivityService activityService;

    @Autowired
    private AccessGuard accessGuard;

    @RequestMapping(value = "", method = RequestMethod.GET)
    public RestResult<PageResult<ActivityItemResponse>> list(
            Context context,
            @Param("page") Integer page,
            @Param("pageSize") Integer pageSize,
            @Param("kind") String kind,
            @Param("operatorId") String operatorId,
            @Param("from") String from,
            @Param("to") String to) {
        return execute(context, () -> {
            // 返回值必须接住：权限点只回答「能不能看审计日志」，
            // 数据范围才回答「能看哪些部门的人留下的日志」。丢掉它等于范围配置静默失效。
            AccessContext access = accessGuard.require(context, Permission.AUDIT_READ);
            return activityService.list(kind, parseId(operatorId), from, to,
                    page == null ? 1 : page, pageSize == null ? 20 : pageSize, access);
        });
    }

    @RequestMapping(value = "/operators", method = RequestMethod.GET)
    public RestResult<List<OperatorOptionResponse>> operators(Context context) {
        return execute(context, () -> {
            AccessContext access = accessGuard.require(context, Permission.AUDIT_READ);
            return activityService.operators(access);
        });
    }

    /** operatorId 不传表示不限，传了就必须是数字。 */
    private static Long parseId(String raw) {
        if (raw == null || raw.trim().isEmpty() || "all".equalsIgnoreCase(raw.trim())) {
            return null;
        }
        try {
            return Long.valueOf(raw.trim());
        } catch (NumberFormatException e) {
            throw new DomainException("操作人参数不合法：" + raw);
        }
    }
}
