package tech.smartboot.feat.demo.plus.dashboard.controller;

import tech.smartboot.feat.cloud.RestResult;
import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Controller;
import tech.smartboot.feat.cloud.annotation.RequestMapping;
import tech.smartboot.feat.cloud.annotation.RequestMethod;
import tech.smartboot.feat.demo.plus.auth.service.AuthGuard;
import tech.smartboot.feat.demo.plus.auth.token.TokenStore;
import tech.smartboot.feat.demo.plus.common.ApiController;
import tech.smartboot.feat.demo.plus.dashboard.service.DashboardService;
import tech.smartboot.feat.router.Context;

@Controller("api/admin")
public class AdminController extends ApiController {

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private TokenStore tokenStore;

    @RequestMapping(value = "/reset-demo", method = RequestMethod.POST)
    public RestResult<Void> resetDemo(Context context) {
        return execute(context, () -> {
            AuthGuard.require(context, tokenStore);
            dashboardService.resetDemo();
            return (Void) null;
        });
    }
}
