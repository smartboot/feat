package tech.smartboot.feat.demo.plus.controller;

import tech.smartboot.feat.cloud.RestResult;
import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Controller;
import tech.smartboot.feat.cloud.annotation.RequestMapping;
import tech.smartboot.feat.cloud.annotation.RequestMethod;
import tech.smartboot.feat.demo.plus.common.ApiController;
import tech.smartboot.feat.demo.plus.dto.DashboardSummaryResponse;
import tech.smartboot.feat.demo.plus.security.AccessContext;
import tech.smartboot.feat.demo.plus.security.AccessGuard;
import tech.smartboot.feat.demo.plus.security.Permission;
import tech.smartboot.feat.demo.plus.service.DashboardService;
import tech.smartboot.feat.router.Context;

@Controller("api/dashboard")
public class DashboardController extends ApiController {

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private AccessGuard accessGuard;

    @RequestMapping(value = "", method = RequestMethod.GET)
    public RestResult<DashboardSummaryResponse> summary(Context context) {
        return execute(context, () -> {
            AccessContext access = accessGuard.require(context, Permission.DASHBOARD_READ);
            return dashboardService.summary(access);
        });
    }
}
