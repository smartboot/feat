package tech.smartboot.feat.demo.plus.dashboard.controller;

import tech.smartboot.feat.cloud.RestResult;
import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Controller;
import tech.smartboot.feat.cloud.annotation.RequestMapping;
import tech.smartboot.feat.cloud.annotation.RequestMethod;
import tech.smartboot.feat.demo.plus.auth.service.AccessGuard;
import tech.smartboot.feat.demo.plus.common.ApiController;
import tech.smartboot.feat.demo.plus.common.Permission;
import tech.smartboot.feat.demo.plus.dashboard.service.DashboardService;
import tech.smartboot.feat.router.Context;

@Controller("api/admin")
public class AdminController extends ApiController {

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private AccessGuard accessGuard;

    /**
     * 重置演示数据会清空所有业务表，属于系统级动作，要求 system:write。
     */
    @RequestMapping(value = "/reset-demo", method = RequestMethod.POST)
    public RestResult<Void> resetDemo(Context context) {
        return execute(context, () -> {
            accessGuard.require(context, Permission.SYSTEM_WRITE);
            dashboardService.resetDemo();
            return (Void) null;
        });
    }
}
