package tech.smartboot.feat.demo.plus.dashboard.controller;

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
import tech.smartboot.feat.demo.plus.dashboard.dto.NotificationItemResponse;
import tech.smartboot.feat.demo.plus.dashboard.service.DashboardService;
import tech.smartboot.feat.router.Context;

@Controller("api/notifications")
public class NotificationController extends ApiController {

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private AccessGuard accessGuard;

    /**
     * 通知是「自己的」数据，只要求登录，不要求额外权限点。
     */
    @RequestMapping(value = "", method = RequestMethod.GET)
    public RestResult<List<NotificationItemResponse>> list(Context context) {
        return execute(context, () -> {
            accessGuard.requireLogin(context);
            return dashboardService.notifications();
        });
    }

    @RequestMapping(value = "/read-all", method = RequestMethod.POST)
    public RestResult<Void> markAllRead(Context context) {
        return execute(context, () -> {
            long operatorId = accessGuard.requireLogin(context).getUserId();
            dashboardService.markAllRead(operatorId);
            return (Void) null;
        });
    }

    @RequestMapping(value = "/{id}/read", method = RequestMethod.POST)
    public RestResult<Void> markRead(Context context, @PathParam("id") String id) {
        return execute(context, () -> {
            long operatorId = accessGuard.requireLogin(context).getUserId();
            dashboardService.markRead(parseId(id), operatorId);
            return (Void) null;
        });
    }

    private static long parseId(String id) {
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException e) {
            throw DomainException.notFound("通知不存在");
        }
    }
}
